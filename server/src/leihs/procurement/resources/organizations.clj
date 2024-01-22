(ns leihs.procurement.resources.organizations
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]))

(def organizations-base-query
  (-> (sql/select :procurement_organizations.*)
      (sql/from :procurement_organizations)
      (sql/order-by [:procurement_organizations.name :asc])))

(defn organizations-query
  [_ args value]
  (let [root-only (:root_only args)
        id (:id value)]
    (sql-format
     (cond-> organizations-base-query
       root-only (sql/where [:= :procurement_organizations.parent_id
                             nil])
       id (sql/where [:= :procurement_organizations.parent_id id])))))

(defn get-organizations
  [context args value]
  (jdbc/execute! (-> context
                     :request
                     :tx-next)
                 (organizations-query context args value)))

(defn delete-unused
  [tx]
  "first delete organizations (parent_id IS NOT NULL) without requesters
  and requests, then delete departments (parent_id IS NULL) without children"
  (jdbc/execute!
   tx
   (-> (sql/select :%count.*)
       (sql/from [:procurement_organizations :po])
       (sql/where :and [:<> :po.parent_id nil]
                  [:not [:exists (-> (sql/select true)
                                     (sql/from [:procurement_requesters_organizations :pro])
                                     (sql/where [:= :pro.organization_id :po.id]))]]
                  [:not [:exists (-> (sql/select true)
                                     (sql/from [:procurement_requests :pr])
                                     (sql/where [:= :pr.organization_id :po.id]))]])
       sql-format))
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_organizations :po)
                     (sql/where :and [:<> :po.parent_id nil]
                                [:not [:exists (-> (sql/select true)
                                                   (sql/from [:procurement_requesters_organizations :pro])
                                                   (sql/where [:= :pro.organization_id :po.id]))]]
                                [:not [:exists (-> (sql/select true)
                                                   (sql/from [:procurement_requests :pr])
                                                   (sql/where [:= :pr.organization_id :po.id]))]])
                     sql-format))
  (jdbc/execute!
   tx
   (-> (sql/delete-from :procurement_organizations :po1)
       (sql/where [:= :po1.parent_id nil]
                  [:not [:exists (-> (sql/select true)
                                     (sql/from [:procurement_organizations :po2])
                                     (sql/where [:= :po2.parent_id :po1.id]))]]) sql-format)))
