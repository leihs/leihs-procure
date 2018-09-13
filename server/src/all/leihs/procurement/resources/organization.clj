(ns leihs.procurement.resources.organization
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(def organization-base-query
  (-> (sql/select :procurement_organizations.*)
      (sql/from :procurement_organizations)))

(defn department-query
  [organization-id]
  (-> (sql/select :procurement_organizations.*)
      (sql/from :procurement_organizations)
      (sql/where [:= :procurement_organizations.id
                  (-> (sql/select :procurement_organizations.parent_id)
                      (sql/from :procurement_organizations)
                      (sql/merge-where [:= :procurement_organizations.id
                                        organization-id]))])
      sql/format))

(def department-base-query
  (-> organization-base-query
      (sql/merge-where [:= :procurement_organizations.parent_id nil])))

(defn department-by-id-query
  [id]
  (-> department-base-query
      (sql/merge-where [:= :procurement_organizations.id id])
      sql/format))

(defn department-by-name-query
  [dep-name]
  (-> department-base-query
      (sql/merge-where [:= :procurement_organizations.name dep-name])
      sql/format))

(defn get-department-by-name
  [tx dep-name]
  (->> dep-name
       department-by-name-query
       (jdbc/query tx)
       first))

(defn get-department-by-id
  [tx id]
  (->> id
       department-by-id-query
       (jdbc/query tx)
       first))

(defn get-organization-by-id
  [tx id]
  (first (jdbc/query tx
                     (-> organization-base-query
                         (sql/merge-where [:= :procurement_organizations.id id])
                         sql/format))))

(defn get-organization
  [context _ value]
  (first (jdbc/query (-> context
                         :request
                         :tx)
                     (-> organization-base-query
                         (sql/merge-where [:= :procurement_organizations.id
                                           (or (:organization_id value)
                                               ; for
                                               ; RequesterOrganization
                                               (:value value)
                                               ; for
                                               ; RequestFieldOrganization
                                             )])
                         sql/format))))

(defn get-organization-by-name-and-dep-id
  [tx org-name dep-id]
  (first
    (jdbc/query
      tx
      (-> organization-base-query
          (sql/merge-where [:= :procurement_organizations.name org-name])
          (sql/merge-where [:= :procurement_organizations.parent_id dep-id])
          sql/format))))

(defn get-department-of-requester-organization
  [context _ value]
  (->> value
       :organization_id
       department-query
       (jdbc/query (-> context
                       :request
                       :tx))
       first))

(defn get-department-of-organization
  [context _ value]
  (let [tx (-> context
               :request
               :tx)]
    (->> value
         :parent_id
         (get-department-by-id tx))))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
