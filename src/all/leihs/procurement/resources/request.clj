(ns leihs.procurement.resources.request
  (:require 
    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [leihs.procurement.utils.sql :as sql]
    [clojure.java.jdbc :as jdbc]
    [logbug.debug :as debug]))

(def state-sql
  (sql/call :case
            [:= :procurement_requests.approved_quantity nil]
            "new"
            [:= :procurement_requests.approved_quantity 0]
            "denied"
            [:and
             [:< 0 :procurement_requests.approved_quantity]
             [:< :procurement_requests.approved_quantity :procurement_requests.requested_quantity]]
            "partially_approved"
            [:>= :procurement_requests.approved_quantity :procurement_requests.requested_quantity]
            "approved"))

(def priorities-mapping {:normal 1
                         :high 2})

(def inspector-priorities-mapping {:low 0
                                   :medium 1
                                   :high 2
                                   :mandatory 3})

(defn remap-priority [row]
  (update row :priority #((keyword %) priorities-mapping)))

(defn remap-inspector-priority [row]
  (update row :inspector_priority #((keyword %) inspector-priorities-mapping)))

(defn add-priority-inspector [row]
  (assoc row :priority_inspector (:inspector_priority row)))

(def row-fn (comp add-priority-inspector 
                  remap-priority
                  remap-inspector-priority))

(defn request-base-query [id]
  (-> (sql/select :* [state-sql :state])
      (sql/from :procurement_requests)
      (sql/where [:= :procurement_requests.id id])
      sql/format))

(defn get-request [context args _]
  (first (jdbc/query (-> context :request :tx)
                     (request-base-query (:request_id args))
                     {:row-fn row-fn})))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
