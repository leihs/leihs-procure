(ns leihs.admin.resources.group.users.back

  (:refer-clojure :exclude [str keyword])
  (:require [leihs.admin.utils.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.group.users.shared :refer [group-users-filter-value]]
    [leihs.admin.resources.users.back :as users]
    [leihs.admin.utils.regex :as regex]
    [leihs.admin.utils.sql :as sql]
    [leihs.admin.utils.jdbc :as utils.jdbc]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [clojure.set :as set]

    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))

(defn normalized-group-id! [group-id tx]
  "Get the id, i.e. the pkey, given either the id or the org_id and
  enforce some sanity checks like uniqueness and presence"
  (assert (presence group-id) "group-id must not be empty!")
  (let [group-id (str group-id)
        where-clause  (if (re-matches regex/uuid-pattern group-id)
                        [:or 
                         [:= :groups.id group-id]
                         [:= :groups.org_id group-id]]
                        [:= :groups.org_id group-id])
        ids (->> (-> (sql/select :id)
                     (sql/from :groups)
                     (sql/merge-where where-clause)
                     sql/format)
                 (jdbc/query tx)
                 (map :id))]
    (assert (= 1 (count ids))
            "exactly one group must match the given group-id either by id or org_id")
    (first ids)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn users-query [group-id request]
  (let [query (-> request users/users-query
                  (sql/merge-left-join :groups_users 
                                       [:and 
                                        [:= :groups_users.user_id :users.id]
                                        [:= :groups_users.group_id group-id]])
                  (sql/merge-select [:groups_users.group_id :group_id]))]
    (if-not (-> request :query-params group-users-filter-value)
      query
      (-> query
          (sql/merge-where 
            [:= :groups_users.group_id group-id])))))


(defn group-users-count-query [group-id]
  (-> (sql/select :%count.*)
      (sql/from :groups_users)
      (sql/merge-where 
        [:= :groups_users.group_id group-id])
      (sql/format)))
  
(defn users-formated-query [group-id request]
  (-> (users-query group-id request)
      sql/format))

(defn users [{{group-id :group-id} :route-params 
              tx :tx :as request}]
  (let [group-id (normalized-group-id! group-id tx)]
    {:body
     {:group_users_count (->> (group-users-count-query group-id)
                              (jdbc/query tx)
                              first :count)
      :users (->> (users-formated-query group-id request)
                  (jdbc/query tx))}}))


;;; update-users ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn users-ids-from-emails-query [emails]
  (-> (sql/select :id)
      (sql/from :users)
      (sql/merge-where [:in :email emails])
      (sql/format)))

(defn- users-ids-from-emails [tx emails]
  (->> emails 
       users-ids-from-emails-query 
       (jdbc/query tx)
       (map :id)
       (map str)))

(defn- users-ids-from-org-ids-query [org-ids]
  (-> (sql/select :id)
      (sql/from :users)
      (sql/merge-where [:in :org_id (map str org-ids)])
      (sql/format)))

(defn- users-ids-from-org-ids [tx org-ids]
  (->> org-ids 
       users-ids-from-org-ids-query 
       (jdbc/query tx)
       (map :id)
       (map str)))

(defn- target-ids [body tx]
  (->> [[]
        (some->> body :emails seq (users-ids-from-emails tx))
        (some->> body :org_ids seq (users-ids-from-org-ids tx))
        (some->> body :ids seq)]
       (apply concat) 
       set))

(defn- existing-ids [group-id tx]
  "returns the current ids of users of a group, 
  group-id must be an existing id (pkey) of a group"
  (->> (-> (sql/select :user_id)
           (sql/from :groups_users)
           (sql/merge-where [:= :group_id group-id])
           (sql/format))
       (jdbc/query tx)
       (map :user_id)
       (map str)
       set))



(defn batch-update-users [{tx :tx body :body 
                           {group-id :group-id} :route-params
                           :as request}]
  (let [group-id (normalized-group-id! group-id tx)
        target-ids (target-ids body tx)
        existing-ids (existing-ids group-id tx)
        to-be-removed-ids (set/difference existing-ids target-ids)
        to-be-added-ids (set/difference target-ids existing-ids)]
    (logging/info 'target-ids target-ids 'existing-ids existing-ids
                  'to-be-removed-ids to-be-removed-ids 
                  'to-be-added-ids to-be-added-ids)
    (when-not (empty? to-be-removed-ids)
      (->> (-> (sql/delete-from :groups_users)
               (sql/merge-where [:= :group_id group-id])
               (sql/merge-where [:in :user_id to-be-removed-ids])
               (sql/format))
           (jdbc/execute! tx)))
    (when-not (empty? to-be-added-ids)
      (jdbc/insert-multi! tx :groups_users
                          (->> to-be-added-ids
                               (map (fn [id] {:group_id group-id
                                              :user_id id})))))
    {:status 200
     :body {:removed-user-ids to-be-removed-ids
            :added-user-ids to-be-added-ids}}))


;;; put-user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn put-user [{tx :tx :as request
                 {group-id :group-id 
                  user-id :user-id} :route-params}]
  (let [group-id (normalized-group-id! group-id tx)]
    (utils.jdbc/insert-or-update!
      tx :groups_users ["group_id = ? AND user_id = ?" group-id user-id]
      {:group_id group-id :user_id user-id})
    {:status 204}))


;;; remove-user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-user [{tx :tx :as request
                    {group-id :group-id 
                     user-id :user-id} :route-params}]
  (let [group-id (normalized-group-id! group-id tx)]
    (if (= 1 (->> ["group_id = ? AND user_id = ?" group-id user-id]
                  (jdbc/delete! tx :groups_users)
                  first))
      {:status 204}
      (throw (ex-info "Remove group-user failed" {:request request})))))
  

;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def group-user-path
  (path :group-user {:group-id ":group-id" :user-id ":user-id"}))

(def group-users-path 
  (path :group-users {:group-id ":group-id"}))

(def routes
  (cpj/routes
    (cpj/PUT group-user-path [] #'put-user)
    (cpj/DELETE group-user-path [] #'remove-user)
    (cpj/GET group-users-path [] #'users)
    (cpj/PUT group-users-path [] #'batch-update-users)))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns 'cider-ci.utils.shutdown)
;(debug/debug-ns *ns*)
