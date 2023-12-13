(ns leihs.procurement.resources.requesters-organizations
  (:require
    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]

    [taoensso.timbre :refer [debug info warn error spy]]


    [leihs.procurement.resources [organization :as organization]
     [organizations :as organizations] [saved-filters :as saved-filters]
     [user :as user]]
    ))

(def requesters-organizations-base-query
  (-> (sql/select :procurement_requesters_organizations.*)
      (sql/from :procurement_requesters_organizations)))

(defn get-requesters-organizations
  [context _ _]
  (jdbc/execute! (-> context
                     :request
                     :tx-next)
                 (sql-format requesters-organizations-base-query)))

(defn get-organization-of-requester
  [tx user-id]
  (-> (sql/select :procurement_organizations.*)
      (sql/from :procurement_requesters_organizations)
      (sql/join :procurement_organizations
                [:= :procurement_requesters_organizations.organization_id :procurement_organizations.id])
      (sql/where [:= :procurement_requesters_organizations.user_id [:cast (spy user-id) :uuid]])
      sql-format
      spy
      (->> (jdbc/execute-one! tx))
      ))

(defn create-requester-organization
  [tx data]
  (let [dep-name (:department data)
        org-name (:organization data)
        p (println ">o> tocheck 1null-values" dep-name org-name)


        department (or (spy (organization/get-department-by-name tx dep-name))
                       (spy (jdbc/execute-one! tx (-> (sql/insert-into :procurement_organizations)
                                                      (sql/values [{:name (spy dep-name)}])
                                                      sql-format
                                                      spy)))
                       )
        p (println ">o> tocheck 2null-values" department)


        organization (or (spy (organization/get-organization-by-name-and-dep-id
                           tx
                           org-name
                           (:id department)))

                         (spy (jdbc/execute-one! tx (-> (sql/insert-into :procurement_organizations)
                                                   (sql/values [{:name org-name,
                                                                 :parent_id [:cast (:id department) :uuid]}])
                                                   sql-format)))
                         )
        p (println ">o> tocheck 3null-values" organization)

        ]

    (jdbc/execute! tx (-> (sql/insert-into :procurement_requesters_organizations)
                          (sql/values [{:user_id [:cast (spy (:user_id data)) :uuid],
                                        :organization_id [:cast (spy (:id organization)) :uuid]}]) ;;FIXME is null
                          sql-format
                          spy))
    ))

(defn delete-all
  [tx]
  (spy (jdbc/execute! tx (->> (sql/delete-from :procurement_requesters_organizations)
                              sql-format
                              spy
                              )))
  )

(defn update-requesters-organizations!
  [context args value]
  (let [tx (-> context
               :request
               :tx-next)]
    (delete-all tx)
    (doseq [d (:input_data args)] (create-requester-organization tx d))
    (organizations/delete-unused tx)
    (saved-filters/delete-unused tx)
    (let [req-orgs
          (jdbc/execute! tx (sql-format requesters-organizations-base-query))]
      (->>
        req-orgs
        (map #(conj %
                    {:organization (->> %
                                        :organization_id
                                        (organization/get-organization-by-id
                                          tx))}))
        (map #(conj %
                    {:department (->> %
                                      :organization
                                      :parent_id
                                      (organization/get-department-by-id tx))}))
        (map #(conj %
                    {:user (->> %
                                :user_id
                                (user/get-user-by-id tx))}))))))
