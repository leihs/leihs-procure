(ns leihs.procurement.graphql
  (:require [clojure.edn :as edn]
            [clojure.java [io :as io] [jdbc :as jdbc]]
            [clojure.tools.logging :as log]
            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia [parser :as graphql-parser]
             [schema :as graphql-schema] [util :as graphql-util]]
            [leihs.procurement.env :as env]
            [leihs.procurement.graphql.resolver :as resolver]
            [leihs.procurement.graphql.helpers :as helpers]
            [leihs.procurement.utils.ring-exception :refer [get-cause]]))

(defn load-schema
  []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (graphql-util/attach-resolvers (resolver/get-resolver-map))
      graphql-schema/compile))

(def schema (load-schema))

(defn get-schema [] (if (#{:dev :test} env/env) (load-schema) schema))

(defn exec-query
  [query-string request]
  (log/debug "graphql query" query-string
             "with variables" (-> request
                                  :body
                                  :variables))
  (lacinia/execute (get-schema)
                   query-string
                   (-> request
                       :body
                       :variables)
                   {:request request}))

(defn pure-handler
  [{{query :query} :body, :as request}]
  (let [result (exec-query query request)]
    (cond-> {:body result} (:errors result) (assoc :graphql-error true))))

(defn parse-query-with-exception-handling
  [schema query]
  (try (graphql-parser/parse-query (get-schema) query)
       (catch Throwable e*
         (let [e (get-cause e*)
               m (.getMessage e*)
               n (-> e*
                     .getClass
                     .getSimpleName)]
           (log/warn (or m n))
           (if (env/env #{:dev :test}) (log/debug e))
           (helpers/error-as-graphql-object "API_ERROR" m)))))

(defn handler
  [{{query :query} :body, :as request}]
  (let [mutation? (->> query
                       (parse-query-with-exception-handling (get-schema))
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
                 (log/warn "Rolling back transaction because of graphql error")
                 (jdbc/db-set-rollback-only! tx))
               response)
             (catch Throwable th
               (log/warn "Rolling back transaction because of " th)
               (jdbc/db-set-rollback-only! tx)
               (throw th))))
      (pure-handler request))))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
