(ns leihs.procurement.graphql
  (:require
    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [com.walmartlabs.lacinia :as lacinia]  
    [leihs.procurement.schema :as schema]
    [logbug.debug :as debug]
    ))

(def schema (schema/load-schema))

(defn exec-query [query-string]
  (lacinia/execute schema query-string nil nil))

(defn handler [{{query :query} :body}]
  {:body (exec-query query)})

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
