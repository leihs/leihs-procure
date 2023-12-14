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
  and requests, then delete departments (parent_id IS NULL) without children       ?? procurement_requesters_organizations cascade delete"

  (println "------ start DELETE-1 -----------------")
  (spy (jdbc/execute!                                       ;; [0]

         ;               ["DELETE FROM procurement_organizations AS po WHERE (po.parent_id IS NOT NULL) AND NOT EXISTS (SELECT TRUE FROM procurement_requesters_organizations AS pro WHERE pro.organization_id = po.id) AND NOT EXISTS (SELECT TRUE FROM procurement_requests AS pr WHERE pr.organization_id = po.id)"]
         ;; master       ["DELETE FROM procurement_organizations po  WHERE (po.parent_id IS NOT NULL AND NOT exists((SELECT TRUE FROM procurement_requesters_organizations pro WHERE pro.organization_id = po.id)) AND NOT exists((SELECT TRUE FROM procurement_requests pr WHERE pr.organization_id = po.id)))"]
         tx (->
              ;(sql/select :*)
              (sql/select :%count.*)

              (sql/from [:procurement_organizations :po])


              (sql/where :and [:<> :po.parent_id nil]
                         [:not [:exists (-> (sql/select true)
                                            (sql/from [:procurement_requesters_organizations :pro])
                                            (sql/where [:= :pro.organization_id :po.id]))]]
                         [:not [:exists (-> (sql/select true)
                                            (sql/from [:procurement_requests :pr])
                                            (sql/where [:= :pr.organization_id :po.id]))]])
              sql-format
              spy
              )))
  (println "------ end DELETE-1 -----------------")

  (spy (jdbc/execute!                                       ;; [0]

         ;               ["DELETE FROM procurement_organizations AS po WHERE (po.parent_id IS NOT NULL) AND NOT EXISTS (SELECT TRUE FROM procurement_requesters_organizations AS pro WHERE pro.organization_id = po.id) AND NOT EXISTS (SELECT TRUE FROM procurement_requests AS pr WHERE pr.organization_id = po.id)"]
         ;; master       ["DELETE FROM procurement_organizations po  WHERE (po.parent_id IS NOT NULL AND NOT exists((SELECT TRUE FROM procurement_requesters_organizations pro WHERE pro.organization_id = po.id)) AND NOT exists((SELECT TRUE FROM procurement_requests pr WHERE pr.organization_id = po.id)))"]
         tx (-> (sql/delete-from :procurement_organizations :po)
                (sql/where :and [:<> :po.parent_id nil]
                           [:not [:exists (-> (sql/select true)
                                              (sql/from [:procurement_requesters_organizations :pro])
                                              (sql/where [:= :pro.organization_id :po.id]))]]

                           [:not [:exists (-> (sql/select true)
                                              (sql/from [:procurement_requests :pr])
                                              (sql/where [:= :pr.organization_id :po.id]))]])

                sql-format
                spy
                )))



  (spy (jdbc/execute! tx (-> (sql/delete-from :procurement_organizations :po1) ;; fixme TODO cause

                             (sql/where [:= :po1.parent_id nil]
                                        [:not [:exists (-> (sql/select true)
                                                           (sql/from [:procurement_organizations :po2])
                                                           (sql/where [:= :po2.parent_id :po1.id]))]]

                                        )

                             sql-format
                             spy))))



