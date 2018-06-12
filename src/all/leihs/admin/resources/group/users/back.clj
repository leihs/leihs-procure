(ns leihs.admin.resources.group.users.back
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.admin.utils.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.group.users.shared :refer [group-users-filter-value]]
    [leihs.admin.resources.users.back :as users]
    [leihs.admin.utils.sql :as sql]
    [leihs.admin.utils.jdbc :as utils.jdbc]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [clojure.set :as set]

    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))


(defn users-query [{:as request
                    {group-id :group-id} :route-params}]
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


(defn group-users-count-query [{{group-id :group-id} :route-params}]
  (-> (sql/select :%count.*)
      (sql/from :groups_users)
      (sql/merge-where 
        [:= :groups_users.group_id group-id])
      (sql/format)))
  
(defn users-formated-query [request]
  (-> request
      users-query
      sql/format))

(defn users [{tx :tx :as request}]
  {:body
   {:group_users_count (->> (group-users-count-query request)
                                 (jdbc/query tx)
                                 first :count)
    :users (->> (users-formated-query request)
                (jdbc/query tx))}})


;;; update-users ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn users-ids-from-emails-query [emails]
  (-> (sql/select :id)
      (sql/from :users)
      (sql/merge-where [:in :email emails])
      (sql/format)))

(defn users-ids-from-emails [tx emails]
  (->> emails 
       users-ids-from-emails-query 
       (jdbc/query tx)
       (map :id)
       (map str)))

(defn users-ids-from-org-ids-query [org-ids]
  (-> (sql/select :id)
      (sql/from :users)
      (sql/merge-where [:in :org_id org-ids])
      (sql/format)))

(defn users-ids-from-org-ids [tx org-ids]
  (->> org-ids 
       users-ids-from-org-ids-query 
       (jdbc/query tx)
       (map :id)
       (map str)))

(defn batch-update-users [{tx :tx body :body 
                           {group-id :group-id} :route-params
                           :as request}]
  (let [target-ids (->> [[]
                         (some->> body :emails seq (users-ids-from-emails tx))
                         (some->> body :org_ids seq (users-ids-from-org-ids tx))
                         (some->> body :ids seq)]
                        (apply concat) 
                        set)
        existing-ids (->> (-> (sql/select :user_id)
                              (sql/from :groups_users)
                              (sql/merge-where [:= :group_id group-id])
                              (sql/format))
                          (jdbc/query tx)
                          (map :user_id)
                          (map str)
                          set)
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
  (utils.jdbc/insert-or-update!
    tx :groups_users ["group_id = ? AND user_id = ?" group-id user-id]
    {:group_id group-id :user_id user-id})
  {:status 204})


;;; remove-user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-user [{tx :tx :as request
                    {group-id :group-id 
                     user-id :user-id} :route-params}]
  (if (= 1 (->> ["group_id = ? AND user_id = ?" group-id user-id]
                (jdbc/delete! tx :groups_users)
                first))
    {:status 204}
    (throw (ex-info "Remove group-user failed" {:request request}))))
  

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
(debug/debug-ns *ns*)
