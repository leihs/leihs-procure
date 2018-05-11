(ns leihs.procurement.graphql
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.tools.logging :as logging]
            [com.walmartlabs.lacinia :as lacinia]
            [leihs.procurement.schema :as schema]
            [logbug.debug :as debug]))

; (def schema (schema/load-schema))

(defn exec-query
  [query-string request]
  (logging/debug "graphql query" query-string
                 "with variables" (-> request
                                      :body
                                      :variables))
  (lacinia/execute (schema/load-schema) ; load schema dynamically for DEBUGGING
                   query-string
                   (-> request
                       :body
                       :variables)
                   {:request request}))

(defn handler
  [{{query :query} :body, :as request}]
  {:body (exec-query query request)})

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
