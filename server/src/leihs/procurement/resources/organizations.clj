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


                ;(sql/where [:<> :po.parent_id nil])
                ;(sql/where [:not [:exists (-> (sql/select true)
                ;                              (sql/from [:procurement_requesters_organizations :pro])
                ;                              (sql/where [:= :pro.organization_id :po.id]))]])
                ;
                ;(sql/where [:not [:exists (-> (sql/select true)
                ;                              (sql/from [:procurement_requests :pr])
                ;                              (sql/where [:= :pr.organization_id :po.id]))]])


                sql-format
                spy
                )))


  ;(Thread/sleep 1000)                                       ; sleep for 1 second


  (println "------ start DELETE-2 -----------------")
  (spy (jdbc/execute! tx (->

                           (sql/select :*)
                           ;(sql/select :%count.*)
                           (sql/from [:procurement_organizations :po1] [:procurement_requesters_organizations :req]) ;; fixme TODO cause


                           (sql/where :and [:= :po1.parent_id nil]
                                      [:= :req.organization_id :po1.id]
                                      [:not [:exists (-> (sql/select true)
                                                         (sql/from [:procurement_organizations :po2])
                                                         (sql/where [:= :po2.parent_id :po1.id]))]])

                           sql-format
                           spy
                           )))

  (println ">>>>>>>>> want to delete")
  (spy (jdbc/execute! tx (->

                           ;(sql/select :*)
                           (sql/select :%count.*)
                           (sql/from [:procurement_organizations :po1]) ;; fixme TODO cause


                           (sql/where :and [:= :po1.parent_id nil]
                                      [:not [:exists (-> (sql/select true)
                                                         (sql/from [:procurement_organizations :po2])
                                                         (sql/where [:= :po2.parent_id :po1.id]))]])

                           sql-format
                           spy
                           )))

  (println ">>>>>>>>> todo")
  (spy (jdbc/execute! tx (->

                           (sql/select :po1.id :po1.name :po1.parent_id)
                           ;(sql/select :%count.*)
                           (sql/from [:procurement_organizations :po1]) ;; fixme TODO cause


                           (sql/where :and [:= :po1.parent_id nil]

                                      ;WHERE procurement_requesters_organizations.organization_id = procurement_organizations.id


                                      ;[:not [:exists (-> (sql/select true)
                                      ;                   (sql/from [:procurement_organizations :po2])
                                      ;                   (sql/inner-join :procurement_requesters_organizations [:= :po2.id :procurement_requesters_organizations.organization_id])
                                      ;                   (sql/where [:= :po2.parent_id :po1.id] [:= :po1.parent_id nil]))]]

                                      [:not [:exists (-> (sql/select true)
                                                         (sql/from [:procurement_requesters_organizations :po3])
                                                         ;(sql/inner-join :procurement_requesters_organizations [:= :po3.id :procurement_requesters_organizations.organization_id])
                                                         ;(sql/where [:= :po3.organization_id :po1.parent_id])
                                                         (sql/where :and [:or [:= :po3.organization_id :po1.parent_id]
                                                                    [:= :po3.organization_id :po1.id]] [[:= :po1.parent_id nil]]
                                                                    )
                                                         )]]
                                      )

                           ;[:in :po1.parent_id nil]
                           ;)
                           ;(sql/where
                           ;  ;:and [:= :po1.parent_id nil]
                           ;           [:not [:exists (-> (sql/select true)
                           ;                              (sql/from [:procurement_organizations :po2])
                           ;                              (sql/inner-join :procurement_requesters_organizations [:= :po2.id :procurement_requesters_organizations.organization_id])
                           ;                              (sql/where [:= :po2.parent_id :po1.parent_id]))]]
                           ;           )

                           sql-format
                           spy)))
  (println "------ end DELETE-2 -----------------")


  ;procurement_requesters_organizations as org
  ;on po1.id =org.organization_id

  ;(println ">>>>>>>>>>> sleeep now")
  ;(Thread/sleep 10000)



  ;["DELETE FROM procurement_organizations AS po1 WHERE (po1.parent_id IS NULL) AND NOT EXISTS (SELECT TRUE FROM procurement_organizations AS po2 WHERE po2.parent_id = po1.id)"]
  ; master => ["DELETE FROM procurement_organizations po1  WHERE (po1.parent_id IS NULL AND NOT exists((SELECT TRUE FROM procurement_organizations po2 WHERE po2.parent_id = po1.id)))"]
  (spy (jdbc/execute! tx (-> (sql/delete-from :procurement_organizations :po1) ;; fixme TODO cause

                             (sql/where :and [:= :po1.parent_id nil]
                                        [:not [:exists (-> (sql/select true)
                                                           (sql/from [:procurement_organizations :po2])
                                                           (sql/where [:= :po2.parent_id :po1.id]))]]

                                        ;[:not [:exists (-> (sql/select true)
                                        ;                   (sql/from [:procurement_organizations :po2])
                                        ;                   (sql/where [:= :po2.parent_id :po1.parent_id]))]]

                                        [:not [:exists (-> (sql/select true)
                                                           (sql/from [:procurement_requesters_organizations :pro])
                                                           (sql/where [:= :pro.organization_id :po1.id]))]]


                                        )


                             ;(sql/where
                             ;           [:not [:in :po1.id (-> (sql/select :po3.id)
                             ;                                  (sql/from [:procurement_organizations :po3])
                             ;                                  (sql/inner-join :procurement_requesters_organizations [:= :po3.id :procurement_requesters_organizations.organization_id])
                             ;                                  (sql/where [:<> :po3.parent_id nil])
                             ;                                  ;(sql/where [:= :po3.parent_id :po1.id])
                             ;                                  )]])

                             sql-format
                             spy))))




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
