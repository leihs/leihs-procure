(ns leihs.procurement.graphql
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia.schema :as graphql-schema]
            [com.walmartlabs.lacinia.util :as graphql-util]
            [leihs.procurement.graphql.resolver :as resolver]
            [logbug.debug :as debug]))

; (def schema (load-schema))

(defn load-schema
  []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (graphql-util/attach-resolvers (resolver/get-resolver-map))
      graphql-schema/compile))

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

(defn handler
  [{{query :query} :body, :as request}]
  (let [result (exec-query query request)]
    (cond-> {:body result} (:errors result) (assoc :graphql-error true))))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
