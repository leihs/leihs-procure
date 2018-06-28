(ns leihs.procurement.resources.request
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.tools.logging :as log]
            [clojure.set :refer [map-invert]]
            [leihs.procurement.authorization :as authorization]
            [leihs.procurement.permissions.request :as request-perms]
            [leihs.procurement.resources.attachments :as attachments]
            [leihs.procurement.resources.model :as model]
            [leihs.procurement.resources.room :as room]
            [leihs.procurement.resources.supplier :as supplier]
            [leihs.procurement.utils.ds :refer [get-ds]]
            [leihs.procurement.utils.sql :as sql]
            [clojure.java.jdbc :as jdbc]
            [logbug.debug :as debug]))

(def attrs-mapping
  {:budget_period :budget_period_id,
   :category :category_id,
   :organization :organization_id,
   :room :room_id,
   :supplier :supplier_id,
   :user :user_id})

(defn exchange-attrs
  ([req] (exchange-attrs req attrs-mapping))
  ([req mapping]
   (reduce (fn [mem [attr1 attr2]]
             (if-let [value (attr1 mem)]
               (-> mem
                   (assoc attr2 value)
                   (dissoc attr1))
               mem))
     req
     mapping)))

(defn reverse-exchange-attrs
  [req]
  (exchange-attrs req (map-invert attrs-mapping)))

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

(defn add-inspector-priority
  [row]
  (assoc row :inspector_priority (:priority_inspector row)))

(defn row-fn
  [tx]
  (comp add-priority-inspector remap-priority remap-inspector-priority))

(def request-base-query
  (-> (sql/select :procurement_requests.* [state-sql :state])
      (sql/from :procurement_requests)))

(defn get-request-by-id
  [tx id]
  (-> request-base-query
      (sql/where [:= :procurement_requests.id id])
      sql/format
      (->> (jdbc/query tx))
      first))

(defn get-request-by-attrs
  [tx attrs]
  (-> request-base-query
      (sql/merge-where (sql/map->where-clause :procurement_requests attrs))
      sql/format
      (->> (jdbc/query tx))
      first))

(defn create-request!
  [context args _]
  (let [input-data (:input_data args)
        ring-req (:request context)
        tx (:tx ring-req)
        auth-user (:authenticated-entity ring-req)
        write-data (or (and (:priority_inspector input-data)
                            (-> input-data
                                add-inspector-priority
                                (dissoc :priority_inspector)))
                       input-data)
        write-data-with-exchanged-attrs (exchange-attrs write-data)]
    (authorization/authorize-and-apply
      #(jdbc/execute! tx
                      (-> (sql/insert-into :procurement_requests)
                          (sql/values [write-data-with-exchanged-attrs])
                          sql/format))
      :if-only
      #(request-perms/authorized-to-write-all-fields? tx auth-user write-data))
    (->> write-data-with-exchanged-attrs
         (get-request-by-attrs tx)
         reverse-exchange-attrs
         (request-perms/apply-permissions tx auth-user))))

(defn update-request!
  [context args _]
  (let [ring-req (:request context)
        tx (:tx ring-req)
        auth-user (:authenticated-entity ring-req)
        input-data (:input_data args)
        req-id (:id input-data)
        proc-request (get-request-by-id tx req-id)
        write-data (let [input-data-without-id (dissoc input-data :id)]
                     (or (and (:priority_inspector input-data-without-id)
                              (-> input-data-without-id
                                  add-inspector-priority
                                  (dissoc :priority_inspector)))
                         input-data-without-id))]
    (authorization/authorize-and-apply
      #(jdbc/execute! tx
                      (-> (sql/update :procurement_requests)
                          (sql/sset write-data)
                          (sql/where [:= :procurement_requests.id req-id])
                          sql/format))
      :if-only
      #(request-perms/authorized-to-write-all-fields? tx
                                                      auth-user
                                                      (reverse-exchange-attrs
                                                        proc-request)
                                                      write-data))
    (->> req-id
         (get-request-by-id tx)
         reverse-exchange-attrs
         (request-perms/apply-permissions tx auth-user))))

(defn delete-request!
  [context args _]
  (let [result (jdbc/execute! (-> context
                                  :request
                                  :tx)
                              (-> (sql/delete-from :procurement_requests)
                                  (sql/where [:= :procurement_requests.id
                                              (-> args
                                                  :input_data
                                                  :id)])
                                  sql/format))]
    (= result '(1))))

(defn requested-by?
  [tx request auth-entity]
  (= (:user_id auth-entity)
     (-> request-base-query
         (sql/merge-where [:= :procurement_requests.id (:id request)])
         sql/format
         (->> (jdbc/query tx))
         first
         :user_id)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
