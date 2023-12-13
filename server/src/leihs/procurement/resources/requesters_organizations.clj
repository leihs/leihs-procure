(ns leihs.procurement.resources.requesters-organizations
  (:require [clojure.java.jdbc :as jdbc]

                [taoensso.timbre :refer [debug info warn error spy]]


            [leihs.procurement.resources [organization :as organization]
             [organizations :as organizations] [saved-filters :as saved-filters]
             [user :as user]]
            [leihs.procurement.utils.sql :as sql]))

(def requesters-organizations-base-query
  (-> (sql/select :procurement_requesters_organizations.*)
      (sql/from :procurement_requesters_organizations)))

(defn get-requesters-organizations
  [context _ _]
  (spy (jdbc/query (-> context
                  :request
                  :tx)
              (sql/format requesters-organizations-base-query))))

(defn get-organization-of-requester
  [tx user-id]
  (spy (-> (sql/select :procurement_organizations.*)
      (sql/from :procurement_requesters_organizations)
      (sql/merge-join :procurement_organizations
                      [:= :procurement_requesters_organizations.organization_id
                       :procurement_organizations.id])
      (sql/merge-where [:= :procurement_requesters_organizations.user_id
                        user-id])
      sql/format
      spy
      (->> (jdbc/query tx))
      first)))

(defn create-requester-organization
  [tx data]
  (let [dep-name (:department data)
        org-name (:organization data)
        p (println ">o> tocheck 1null-values, dep-name=" dep-name ) ;; dep-name= Rektorat
        p (println ">o> tocheck 1null-values, org-name="  org-name) ;; org-name= Rektorat

        department (or (spy (organization/get-department-by-name tx dep-name)) ;; {:id #uuid "a80d7883-9e83-5d93-b7b7-11af0eee9142", :name "DDK", :shortname nil, :parent_id nil}
                       (spy (first (jdbc/insert! tx
                                            :procurement_organizations
                                            {:name dep-name}))))
        organization (or (spy (organization/get-organization-by-name-and-dep-id ;; {:id #uuid "9161d8e2-9431-4bf7-8fea-279a6148c941", :name "Forschung Cultural Critique", :shortname nil, :parent_id #uuid "53266f06-6625-5967-9c39-d8e1941d8ae2"}
                           tx
                           org-name
                           (:id department)))
                         (spy (first (jdbc/insert! tx
                                              :procurement_organizations
                                              {:name org-name,
                                               :parent_id (:id department)}))))]
    (spy (jdbc/insert! tx                                   ;;  => ({:id #uuid "5f73cbf5-61a2-41e9-806f-02f424d7291e", :user_id #uuid "fefcc6b2-4fb6-5e0c-8795-b4988c557c01", :organization_id #uuid "9161d8e2-9431-4bf7-8fea-279a6148c941"})
                  :procurement_requesters_organizations
                  {:user_id (:user_id data),
                   :organization_id (:id organization)}))))

(defn delete-all
  [tx]
  (spy (jdbc/delete! tx :procurement_requesters_organizations [])))

(defn update-requesters-organizations!
  [context args value]
  (let [tx (-> context
               :request
               :tx)]
    (delete-all tx)
    (doseq [d (:input_data args)] (create-requester-organization tx d))
    (organizations/delete-unused tx)
    (saved-filters/delete-unused tx)
    (let [req-orgs
            (spy (jdbc/query tx (sql/format requesters-organizations-base-query)))]
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
