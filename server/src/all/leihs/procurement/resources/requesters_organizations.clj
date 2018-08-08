(ns leihs.procurement.resources.requesters-organizations
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.resources [organization :as organization]
             [organizations :as organizations] [saved-filters :as saved-filters]
             [user :as user]]
            [leihs.procurement.utils.sql :as sql]))

(def requesters-organizations-base-query
  (-> (sql/select :procurement_requesters_organizations.*)
      (sql/from :procurement_requesters_organizations)))

(defn get-requesters-organizations
  [context _ _]
  (jdbc/query (-> context
                  :request
                  :tx)
              (sql/format requesters-organizations-base-query)))

(defn get-organization-of-requester
  [tx user-id]
  (-> (sql/select :procurement_organizations.*)
      (sql/from :procurement_requesters_organizations)
      (sql/merge-join :procurement_organizations
                      [:= :procurement_requesters_organizations.organization_id
                       :procurement_organizations.id])
      (sql/merge-where [:= :procurement_requesters_organizations.user_id
                        user-id])
      sql/format
      (->> (jdbc/query tx))
      first))

(defn create-requester-organization
  [tx data]
  (let [dep-name (:department data)
        org-name (:organization data)
        department (or (organization/get-department-by-name tx dep-name)
                       (first (jdbc/insert! tx
                                            :procurement_organizations
                                            {:name dep-name})))
        organization (or (organization/get-organization-by-name-and-dep-id
                           tx
                           org-name
                           (:id department))
                         (first (jdbc/insert! tx
                                              :procurement_organizations
                                              {:name org-name,
                                               :parent_id (:id department)})))]
    (jdbc/insert! tx
                  :procurement_requesters_organizations
                  {:user_id (:user_id data),
                   :organization_id (:id organization)})))

(defn delete-all
  [tx]
  (jdbc/delete! tx :procurement_requesters_organizations []))

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
            (jdbc/query tx (sql/format requesters-organizations-base-query))]
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

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
