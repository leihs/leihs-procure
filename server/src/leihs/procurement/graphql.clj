(ns leihs.procurement.graphql
  (:require
    [clojure.edn :as edn]
    (clojure.java [io :as io])
    (clojure.java [io :as io])
    ;; all needed imports
    [com.walmartlabs.lacinia :as lacinia]
    (com.walmartlabs.lacinia [parser :as graphql-parser]
                             [schema :as graphql-schema] [util :as graphql-util])
    [leihs.core.db :as db]
    [leihs.core.graphql :as core-graphql]
    [leihs.procurement.graphql.helpers :as helpers]
    [leihs.procurement.graphql.resolver :as resolver]
    [leihs.procurement.utils.ring-exception :refer [get-cause]]
    [next.jdbc :as jdbc]
    [taoensso.timbre :refer [debug error info spy warn]]))

(def CUSTOM_SCALARS
  {:ID {:parse identity :serialize str}
   :Int {:parse (fn [v]
                  (if (number? v) v (Integer/parseInt v)))
         :serialize identity}})

(defn load-schema! []
  (or (some-> (io/resource "schema.edn")
              slurp edn/read-string
              (graphql-util/attach-resolvers resolver/resolvers)
              (assoc-in [:scalars] CUSTOM_SCALARS)
              graphql-schema/compile)
      (throw (ex-info "Failed to load schema" {}))))

(defn exec-query
  [query-string request]
  (debug "graphql query" query-string
         "with variables" (-> request
                              :body
                              :variables))
  (lacinia/execute (core-graphql/schema)
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
                       (parse-query-with-exception-handling (core-graphql/schema))
                       graphql-parser/operations
                       :type
                       (= :mutation))]
    (if mutation?
      (jdbc/with-transaction+options [tx-next (db/get-ds-next)]
                                     (letfn [(rollback-both-tx! []
                                               (.rollback (:connectable tx-next)))]
                                       (try (let [response (-> request
                                                               (assoc :tx-next tx-next)
                                                               pure-handler)]
                                              (when (:graphql-error response)
                                                (warn "Rolling back transaction because of graphql error: " response)
                                                (rollback-both-tx!))
                                              response)
                                            (catch Throwable th
                                              (warn "Rolling back transaction because of " th)
                                              (rollback-both-tx!)
                                              (throw th)))))
      (pure-handler request))))


(defn init []
  (core-graphql/init-schema! (load-schema!)))
