(ns leihs.procurement.graphql
  (:require
    [clojure.edn :as edn]
    [clojure.java [io :as io] [jdbc :as jdbc]]
    [com.walmartlabs.lacinia :as lacinia]
    [com.walmartlabs.lacinia [parser :as graphql-parser]
     [schema :as graphql-schema] [util :as graphql-util]]
    [leihs.procurement.graphql.resolver :as resolver]
    [leihs.procurement.graphql.helpers :as helpers]
    [leihs.procurement.utils.ring-exception :refer [get-cause]]
    [taoensso.timbre :refer [debug info warn error spy]]
    ))

(def CUSTOM_SCALARS
  {:ID {:parse identity :serialize str}
   :Int {:parse (fn [v]
                  (if (number? v) v (Integer/parseInt v)))
         :serialize identity}})

(def schema* (atom nil))

(defn load-schema! []
  (or (some-> (io/resource "schema.edn")
              slurp edn/read-string
              (graphql-util/attach-resolvers resolver/resolvers)
              (assoc-in [:scalars] CUSTOM_SCALARS)
              graphql-schema/compile)
      (throw (ex-info "Failed to load schema" {}))))

(defn init-schema! []
  (reset! schema* (load-schema!))
  (or @schema* ))

(defn schema []
  (or @schema* (throw (ex-info  "Schema not initialized " {}))))

(defn exec-query
  [query-string request]
  (debug "graphql query" query-string
             "with variables" (-> request
                                  :body
                                  :variables))
  (lacinia/execute (schema)
                   query-string
                   (-> request
                       :body
                       :variables)
                   {:request request}))

(defn pure-handler
  [{{query :query} :body, :as request}]
  (let [result (exec-query query request)
        resp {:body result}]
    (if (:errors result)
      (do (debug result) (assoc resp :graphql-error true))
      resp)))

(defn parse-query-with-exception-handling
  [schema query]
  (try (graphql-parser/parse-query schema query)
       (catch Throwable e*
         (let [e (get-cause e*)
               m (.getMessage e*)
               n (-> e*
                     .getClass
                     .getSimpleName)]
           (warn (or m n))
           (debug e)
           (helpers/error-as-graphql-object "API_ERROR" m)))))

(defn handler
  [{{query :query} :body, :as request}]
  (let [mutation? (->> query
                       (parse-query-with-exception-handling (schema))
                       graphql-parser/operations
                       :type
                       (= :mutation))]
    (if mutation?
      (jdbc/with-db-transaction
        [tx (:tx request)]
        (try (let [response (->> tx
                                 (assoc request :tx)
                                 pure-handler)]
               (when (:graphql-error response)
                 (warn "Rolling back transaction because of graphql error: " response)
                 (jdbc/db-set-rollback-only! tx))
               response)
             (catch Throwable th
               (warn "Rolling back transaction because of " th)
               (jdbc/db-set-rollback-only! tx)
               (throw th))))
      (pure-handler request))))


(defn init []
  (info "Initializing graphQL schema...")
  (init-schema!)
  (info "initialized graphQL schema."))
