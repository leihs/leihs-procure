(ns leihs.procurement.resources.request-fields
  (:require 
    [clj-logging-config.log4j :as logging-config]
    [clojure.java.jdbc :as jdbc]
    [clojure.tools.logging :as logging]
    [leihs.procurement.permissions.request-fields :as rf-perms]  
    [leihs.procurement.resources.request :as request]
    [leihs.procurement.utils.sql :as sql]
    ))

(defn get-request-fields [context arguments value]
  (let [request (request/get-request context arguments value)
        rf-perms (rf-perms/all-for-user-and-request context arguments value)]
    (logging/debug request)
    (logging/debug rf-perms)
    (map (fn [[k v]] (merge v {:name k, :value (k request)}))
         (seq rf-perms))))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
