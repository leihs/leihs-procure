(ns leihs.procurement.resources.organization
  (:require
    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]

        [taoensso.timbre :refer [debug info warn error spy]]


    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]

    ))

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
                      (sql/where [:= :procurement_organizations.id
                                  [:cast organization-id :uuid]]))])
      sql-format))

(def department-base-query
  (-> organization-base-query
      (sql/where [:= :procurement_organizations.parent_id nil])))

(defn department-by-id-query
  [id]
  (-> department-base-query
      (sql/where [:= :procurement_organizations.id [:cast id :uuid]])
      sql-format))

(defn department-by-name-query
  [dep-name]
  (-> department-base-query
      (sql/where [:= :procurement_organizations.name dep-name])
      sql-format))

(defn get-department-by-name
  [tx dep-name]
  (spy (->> dep-name
       department-by-name-query
       (jdbc/execute-one! tx)
       )))

(defn get-department-by-id
  [tx id]
  (->> id
       department-by-id-query
       (jdbc/execute-one! tx)
       ))

(defn get-organization-by-id
  [tx id]
  (jdbc/execute-one! tx
                     (-> organization-base-query
                         (sql/where [:= :procurement_organizations.id [:cast id :uuid]])
                         sql-format)))

(defn get-organization
  [context _ value]
  (jdbc/execute-one! (-> context
                         :request
                         :tx-next)
                     (-> organization-base-query
                         (sql/where [:= :procurement_organizations.id
                                     (or (:organization_id value)
                                         ; for
                                         ; RequesterOrganization
                                         (:value value)
                                         ; for
                                         ; RequestFieldOrganization
                                         )])
                         sql-format
                         spy
                         )))

(defn get-organization-by-name-and-dep-id
  [tx org-name dep-id]

  (jdbc/execute-one!
    tx
    (-> organization-base-query
        (sql/where [:and [:= :procurement_organizations.name org-name]
                    [:= :procurement_organizations.parent_id dep-id]])
        sql-format)))

(defn get-department-of-requester-organization
  [context _ value]
  (->> value
       :organization_id
       department-query
       (jdbc/execute-one! (-> context
                              :request
                              :tx-next))
       ))

(defn get-department-of-organization
  [context _ value]
  (let [tx (-> context
               :request
               :tx-next)]
    (->> value
         :parent_id
         (get-department-by-id tx))))
