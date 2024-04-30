(ns leihs.procurement.resources.requesters-organizations
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   (leihs.procurement.resources [organization :as organization]
                                [organizations :as organizations] [saved-filters :as saved-filters]
                                [user :as user])
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]))

(def requesters-organizations-base-query
  (-> (sql/select :procurement_requesters_organizations.*)
      (sql/from :procurement_requesters_organizations)))

(defn get-requesters-organizations
  [context _ _]
  (jdbc/execute! (-> context
                     :request
                     :tx)
                 (sql-format requesters-organizations-base-query)))

(defn get-organization-of-requester
  [tx user-id]
  (let [query (-> (sql/select :procurement_organizations.*)
                  (sql/from :procurement_requesters_organizations)
                  (sql/join :procurement_organizations
                            [:= :procurement_requesters_organizations.organization_id :procurement_organizations.id])
                  (sql/where [:= :procurement_requesters_organizations.user_id user-id])
                  sql-format)]
    (jdbc/execute-one! tx query)))

(defn create-requester-organization
  [tx data]
  (let [dep-name (:department data)
        org-name (:organization data)
        department (or (organization/get-department-by-name tx dep-name) ;; nil
                       (jdbc/execute-one! tx (-> (sql/insert-into :procurement_organizations)
                                                 (sql/values [{:name dep-name}])
                                                 (sql/returning :*)
                                                 sql-format)))
        organization (or (organization/get-organization-by-name-and-dep-id ;;
                          tx
                          org-name
                          (:id department))
                         (jdbc/execute-one! tx (-> (sql/insert-into :procurement_organizations)
                                                   (sql/values [{:name org-name,
                                                                 :parent_id (:id department)}])
                                                   (sql/returning :*)
                                                   sql-format)))]
    (jdbc/execute! tx (-> (sql/insert-into :procurement_requesters_organizations)
                          (sql/values [{:user_id (:user_id data),
                                        :organization_id (:id organization)}])
                          (sql/returning :*)
                          sql-format))))

(defn delete-all
  [tx]
  (jdbc/execute! tx (->> (sql/delete-from :procurement_requesters_organizations)
                         sql-format)))

(defn update-requesters-organizations!
  [context args value]
  (let [tx (-> context
               :request
               :tx)]
    (delete-all tx)
    (doseq [d (:input_data args)] (create-requester-organization tx d))
    (organizations/delete-unused tx)
    (saved-filters/delete-unused tx)
    (let [req-orgs (jdbc/execute! tx (sql-format requesters-organizations-base-query))]
      (->> req-orgs
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
