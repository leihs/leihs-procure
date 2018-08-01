(ns leihs.procurement.graphql
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia.parser :as graphql-parser]
            [com.walmartlabs.lacinia.schema :as graphql-schema]
            [com.walmartlabs.lacinia.util :as graphql-util]
            [leihs.procurement.graphql.resolver :as resolver]
            [leihs.procurement.utils.ds :as ds]
            [logbug.debug :as debug]))

; ===========================================================================
; FIXME: use the defed var instead of calling this function on every request
(defn load-schema
  []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (graphql-util/attach-resolvers (resolver/get-resolver-map))
      graphql-schema/compile))

(def schema (load-schema))
; ===========================================================================

(defn exec-query
  [query-string request]
  (log/debug "graphql query" query-string
             "with variables" (-> request
                                  :body
                                  :variables))
  (lacinia/execute (load-schema) ; load schema dynamically for DEBUGGING
                   query-string
                   (-> request
                       :body
                       :variables)
                   {:request request}))

(defn pure-handler
  [{{query :query} :body, :as request}]
  (let [result (exec-query query request)]
    (cond-> {:body result} (:errors result) (assoc :graphql-error true))))

(defn handler
  [{{query :query} :body, :as request}]
  (let [mutation? (->> query
                       (graphql-parser/parse-query schema)
                       graphql-parser/operations
                       :type
                       (= :mutation))]
    (if mutation?
      (jdbc/with-db-transaction
        [tx (:tx request)]
        (try (let [response (->> tx
                                 (assoc request :tx)
                                 pure-handler
                                 (hash-map :body))]
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
