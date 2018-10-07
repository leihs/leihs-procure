(ns leihs.admin.resources.system-admins.direct-users.back
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.sql :as sql]

    [leihs.admin.auth.back :as admin-auth]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.system-admins.direct-users.shared :refer [filter-value]]
    [leihs.admin.resources.users.back :as users]
    [leihs.admin.utils.regex :as regex]
    [leihs.admin.utils.jdbc :as utils.jdbc]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [clojure.set :as set]

    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn users-query [request]
  (let [query (-> request users/users-query
                  (sql/merge-left-join :system_admin_users
                                       [:= :system_admin_users.user_id :users.id])
                  (sql/merge-select [:system_admin_users.user_id :system_admin_user_id]))]
    (if-not (-> request :query-params-raw filter-value)
      query
      (sql/merge-where query [:<> :system_admin_users.user_id nil]))))


(def system-admin-direct-users-count-query
  (-> (sql/select :%count.*)
      (sql/from :system_admin_users)
      (sql/format)))

(defn users-formated-query [request]
  (-> (users-query request)
      sql/format))

(defn users [{tx :tx :as request}]
    {:body
     {:system-admin_users_count (->> system-admin-direct-users-count-query
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

(defn- existing-ids [system-admin-id tx]
  "returns the current ids of users of a system-admin,
  system-admin-id must be an existing id (pkey) of a system-admin"
  (->> (-> (sql/select :user_id)
           (sql/from :system-admins_users)
           (sql/merge-where [:= :system-admin_id system-admin-id])
           (sql/format))
       (jdbc/query tx)
       (map :user_id)
       (map str)
       set))



(defn batch-update-users [{tx :tx body :body
                           {system-admin-id :system-admin-id} :route-params
                           :as request}]
  (let [target-ids (target-ids body tx)
        existing-ids (existing-ids system-admin-id tx)
        to-be-removed-ids (set/difference existing-ids target-ids)
        to-be-added-ids (set/difference target-ids existing-ids)]
    (logging/info 'target-ids target-ids 'existing-ids existing-ids
                  'to-be-removed-ids to-be-removed-ids
                  'to-be-added-ids to-be-added-ids)
    (when-not (empty? to-be-removed-ids)
      (->> (-> (sql/delete-from :system-admins_users)
               (sql/merge-where [:= :system-admin_id system-admin-id])
               (sql/merge-where [:in :user_id to-be-removed-ids])
               (sql/format))
           (jdbc/execute! tx)))
    (when-not (empty? to-be-added-ids)
      (jdbc/insert-multi! tx :system-admins_users
                          (->> to-be-added-ids
                               (map (fn [id] {:system-admin_id system-admin-id
                                              :user_id id})))))
    {:status 200
     :body {:removed-user-ids to-be-removed-ids
            :added-user-ids to-be-added-ids}}))


;;; put-user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn put-user [{tx :tx :as request
                 body :body
                 {user-id :user-id} :route-params}]
  (utils.jdbc/insert-or-update!
    tx :system_admin_users
    ["user_id = ?" user-id]
    {:user_id user-id})
  {:status 204})


;;; remove-user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-user [{tx :tx :as request
                    {user-id :user-id} :route-params}]
  (if (= 1 (->> ["user_id = ?" user-id]
                (jdbc/delete! tx :system_admin_users)
                first))
    {:status 204}
    (throw (ex-info "Remove system_admin_users failed" {:status 409}))))


;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def system-admins-direct-user-path
  (path :system-admins-direct-user {:user-id ":user-id"}))

(def system-admin-direct-users-path
  (path :system-admin-direct-users {}))

(def routes
  (-> (cpj/routes
        (cpj/PUT system-admins-direct-user-path [] #'put-user)
        (cpj/DELETE system-admins-direct-user-path [] #'remove-user)
        (cpj/GET system-admin-direct-users-path [] #'users)
        (cpj/PUT system-admin-direct-users-path [] #'batch-update-users))
      (admin-auth/wrap-authorize #{} {:scope_admin_read true
                                      :scope_admin_write true
                                      :scope_system_admin_read true
                                      :scope_system_admin_write true})))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
;(debug/debug-ns 'leihs.admin.resources.system-admins.direct-users.shared)
