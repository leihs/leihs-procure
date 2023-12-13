(ns leihs.procurement.resources.organizations
  (:require

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]

    [taoensso.timbre :refer [debug info warn error spy]]



    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]
    ))

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
              id (sql/where [:= :procurement_organizations.parent_id [:cast id :uuid]])))))

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
  (spy (jdbc/execute! tx (-> (sql/delete-from :procurement_organizations :po)
                             (sql/where [:<> :po.parent_id nil])
                             (sql/where [:not [:exists (-> (sql/select true)
                                                           (sql/from [:procurement_requesters_organizations :pro])
                                                           (sql/where [:= :pro.organization_id :po.id]))]])

                             (sql/where [:not [:exists (-> (sql/select true)
                                                           (sql/from [:procurement_requests :pr])
                                                           (sql/where [:= :pr.organization_id :po.id]))]])
                             sql-format
                             spy
                             )))


  (spy (jdbc/execute! tx (-> (sql/delete-from :procurement_organizations :po1) ;; fixme TODO cause
                             (sql/where [:= :po1.parent_id nil])
                             (sql/where [:not [:exists (-> (sql/select true)
                                                           (sql/from [:procurement_organizations :po2])
                                                           (sql/where [:= :po2.parent_id :po1.id]))]])
                             sql-format
                             spy
                             ))))




(comment

  (let [
        tx (db/get-ds-next)
        request {:route-params {:user-id #uuid "c0777d74-668b-5e01-abb5-f8277baa0ea8"}
                 :tx tx}
        user-id #uuid "37bb3d3d-3a61-4f98-863e-c549568317f0"
        ;query (sql-format {:select :*
        ;                   :from [:users]
        ;                   :where [:= :id [:cast user-id :uuid]]})


        ;query2 (spy (-> (sql/select :*)
        query2 (spy (-> (sql/delete-from :procurement_organizations :po) ;; FIX,with [] => DELETE FROM PROCUREMENT_ORGANIZATIONS(po)
                        ;(sql/from [:procurement_organizations :po])
                        (sql/where [:<> :po.parent_id nil])
                        (sql/where [:not [:exists (-> (sql/select true)
                                                      (sql/from [:procurement_requesters_organizations :pro])
                                                      (sql/where [:= :pro.organization_id :po.id]))]])

                        (sql/where [:not [:exists (-> (sql/select true)
                                                      (sql/from [:procurement_requests :pr])
                                                      (sql/where [:= :pr.organization_id :po.id]))]])
                        sql-format
                        ;spy
                        ))


        ;query2 (jdbc/execute! tx query2)


        ;query2 (-> (sql/select :*)
        ;           (sql/from :users)
        ;
        ;           ;(sql/where [:= :id user-id])
        ;
        ;           ;SELECT * FROM users a WHERE NOT EXISTS (SELECT TRUE FROM users b WHERE b.login is NULL and a.id=b.id);
        ;
        ;           (sql/where [:not [:exists (-> (sql/select true)
        ;                                         (sql/from :users)
        ;                                         (sql/where [:= :login nil]))]])
        ;
        ;
        ;
        ;           sql-format
        ;           spy
        ;           (->> (jdbc/execute! tx))
        ;           )





        ;p (println "\nquery" query)
        p (println "\nquery2" query2)
        ]

    )
  )
