(ns leihs.procurement.resources.requesters-organizations
  (:require 
    [clj-logging-config.log4j :as logging-config]
    [clojure.java.jdbc :as jdbc]
    [clojure.tools.logging :as logging]
    [leihs.procurement.resources.request :as request]
    [leihs.procurement.utils.sql :as sql]
    [logbug.debug :as debug]))

(def requesters-organizations-base-query
  (-> (sql/select :procurement_requesters_organizations.*)
      (sql/from :procurement_requesters_organizations)))

(defn get-requesters-organizations [context _ _]
  (jdbc/query (-> context :request :tx)
              (sql/format requesters-organizations-base-query)))
  
;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
