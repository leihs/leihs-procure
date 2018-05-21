(ns leihs.procurement.resources.request
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.tools.logging :as log]
            [leihs.procurement.permissions.request-fields :as
             request-fields-perms]
            [leihs.procurement.resources.attachments :as attachments]
            [leihs.procurement.resources.model :as model]
            [leihs.procurement.resources.room :as room]
            [leihs.procurement.resources.supplier :as supplier]
            [leihs.procurement.utils.ds :as ds]
            [leihs.procurement.utils.sql :as sql]
            [clojure.java.jdbc :as jdbc]
            [logbug.debug :as debug]))

(def state-sql
  (sql/call :case
            [:= :procurement_requests.approved_quantity nil]
            "new"
            [:= :procurement_requests.approved_quantity 0]
            "denied"
            [:and [:< 0 :procurement_requests.approved_quantity]
             [:< :procurement_requests.approved_quantity
              :procurement_requests.requested_quantity]]
            "partially_approved"
            [:>= :procurement_requests.approved_quantity
             :procurement_requests.requested_quantity]
            "approved"))

(def priorities-mapping {:normal 1, :high 2})

(def inspector-priorities-mapping {:low 0, :medium 1, :high 2, :mandatory 3})

(defn remap-priority
  [row]
  (update row :priority #((keyword %) priorities-mapping)))

(defn remap-inspector-priority
  [row]
  (update row :inspector_priority #((keyword %) inspector-priorities-mapping)))

(defn add-priority-inspector
  [row]
  (assoc row :priority_inspector (:inspector_priority row)))

(defn embed-room
  [tx row]
  (->> row
       :room_id
       (room/get-room-by-id tx)
       (assoc row :room)))

(defn embed-model
  [tx row]
  (->> row
       :model_id
       (model/get-model-by-id tx)
       (assoc row :model)))

(defn embed-supplier
  [tx row]
  (->> row
       :supplier_id
       (supplier/get-supplier-by-id tx)
       (assoc row :supplier)))

(defn embed-attachments
  [tx row]
  (->> row
       :id
       (attachments/get-attachments-for-request-id tx)
       (assoc row :attachments)))

(defn row-fn
  [tx]
  (comp add-priority-inspector
        #(embed-attachments tx %)
        #(embed-model tx %)
        #(embed-room tx %)
        #(embed-supplier tx %)
        remap-priority
        remap-inspector-priority))

(defn request-base-query
  [id]
  (-> (sql/select :procurement_requests* [state-sql :state])
      (sql/from :procurement_requests)
      (sql/where [:= :procurement_requests.id id])
      sql/format))

(defn apply-permissions
  [tx auth-user proc-request]
  (let [proc-request-perms (request-fields-perms/get-for-user-and-request
                             tx
                             auth-user
                             proc-request)]
    (into {}
          (map (fn [[attr value]]
                 {attr (if-let [p-spec (attr proc-request-perms)]
                         (and (:read p-spec) (assoc p-spec :value value))
                         value)})
            proc-request))))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
