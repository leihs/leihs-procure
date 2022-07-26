(ns leihs.admin.resources.groups.group.users.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.core.sql :as sql]
    [leihs.core.auth.core :as auth]

    [leihs.admin.common.users-and-groups.core :as  users-and-groups]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.groups.group.main :as group]
    [leihs.admin.resources.groups.group.users.shared :refer [default-query-params]]
    [leihs.admin.resources.users.main :as users]

    [leihs.admin.utils.jdbc :as utils.jdbc]
    [leihs.admin.utils.regex :as regex]
    [leihs.admin.utils.seq :as seq]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [clojure.set :as set]

    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))

(defn protected-checked-group! [request]
  (let [group (-> request group/get-group :body)]
    (when-not group
      (throw (ex-info "Group not found" {:status 404})))
    (when-not (-> request auth/system-admin-scopes?)
      (users-and-groups/assert-not-system-admin-proteced! group))
    (when-not (-> request auth/admin-scopes?)
      (users-and-groups/assert-not-admin-proteced! group))
    group))

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


(defn base-users-query [group-id request]
  (-> request users/users-query
      (sql/merge-left-join :groups_users
                           [:and
                            [:= :groups_users.user_id :users.id]
                            [:= :groups_users.group_id group-id]])
      (sql/merge-select [:groups_users.group_id :group_id])))

(defn users-query [group-id {query-params :query-params :as request}]
  (let [query (base-users-query group-id request)]
    (case (or (-> query-params :membership presence)
              (-> default-query-params :membership))
      "any" query
      ("yes" "member")  (-> query
                            (sql/merge-where
                              [:= :groups_users.group_id group-id])))))



(defn users [{{group-id :group-id} :route-params
              tx :tx :as request}]
  (let [group-id (normalized-group-id! group-id tx)
        query (users-query group-id request)
        offset (:offset query)]
    {:body
     {:users  (-> query sql/format
                  (->> (jdbc/query tx)
                       (seq/with-index offset)
                       seq/with-page-index))}}))


;;; update-users ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



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
  (when-let [extra-keys (some-> body keys set (disj :ids) not-empty)]
    (throw (ex-info (str "Batch uptdate only supports the ids key, given: "
                         extra-keys) {:status 422})))
  (let [group (protected-checked-group! request)
        group-id (:id group)
        target-ids (-> body (get :ids) set)
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
  (let [group (protected-checked-group! request)
        group-id (:id group)]
    (utils.jdbc/insert-or-update!
      tx :groups_users ["group_id = ? AND user_id = ?" group-id user-id]
      {:group_id group-id :user_id user-id})
    {:status 204}))


;;; remove-user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-user [{tx :tx :as request
                    {group-id :group-id
                     user-id :user-id} :route-params}]
  (let [group (protected-checked-group! request)
        group-id (:id group) ]
    (if (= 1 (->> ["group_id = ? AND user_id = ?" group-id user-id]
                  (jdbc/delete! tx :groups_users)
                  first))
      {:status 204}
      (throw (ex-info (str "Remove group-user failed. "
                           "It seems the user is not a member of the group.")
                      {:status 404})))))


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


;(debug/debug-ns *ns*)
