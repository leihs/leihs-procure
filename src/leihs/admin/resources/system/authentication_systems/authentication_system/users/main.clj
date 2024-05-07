(ns leihs.admin.resources.system.authentication-systems.authentication-system.users.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [bidi.bidi :refer [match-route]]
   [clojure.core.match :refer [match]]
   [clojure.set :as set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.common.membership.users.main :refer [extend-with-membership]]
   [leihs.admin.paths :refer [path paths]]
   [leihs.admin.resources.users.main :as users]
   [leihs.admin.utils.jdbc :as utils.jdbc]
   [leihs.admin.utils.seq :as seq]
   [leihs.core.core :refer [str]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [delete! insert-multi! query] :rename {query jdbc-query,
                                                                delete! jdbc-delete!,
                                                                insert-multi! jdbc-insert-multi!}]))

(defn direct-member-expr [authentication-system-id]
  [:exists
   (-> (sql/select true)
       (sql/from :authentication_systems_users)
       (sql/where [:= :users.id :authentication_systems_users.user_id])
       (sql/where [:= :authentication_systems_users.authentication_system_id
                   authentication-system-id]))])

(defn group-member-expr [authentication-system-id]
  [:exists
   (-> (sql/select true)
       (sql/from :authentication_systems_groups)
       (sql/where [:= :authentication_systems_groups.authentication_system_id
                   authentication-system-id])
       (sql/join :groups [:= :authentication_systems_groups.group_id :groups.id])
       (sql/join :groups_users [:= :groups_users.group_id :groups.id])
       (sql/where [:= :users.id :groups_users.user_id]))])

(defn member-expr [authentication-system-id]
  [:or
   (direct-member-expr authentication-system-id)
   (group-member-expr authentication-system-id)])

(defn users-query
  [{{authentication-system-id :authentication-system-id} :route-params
    :as request}]
  (-> (users/users-query request)
      (extend-with-membership
       (member-expr authentication-system-id)
       (direct-member-expr authentication-system-id)
       (group-member-expr authentication-system-id)
       request)))

(defn users [{tx :tx :as request}]
  (let [query (users-query request)
        offset (:offset query)]
    {:body
     {:users (->> query
                  sql-format
                  (jdbc-query tx)
                  (seq/with-index offset)
                  seq/with-page-index)}}))

;;; update-users ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn users-ids-from-emails-query [emails]
  (-> (sql/select :id)
      (sql/from :users)
      (sql/where [:in :email emails])
      (sql-format)))

(defn- users-ids-from-emails [tx emails]
  (->> emails
       users-ids-from-emails-query
       (jdbc-query tx)
       (map :id)
       (map str)))

(defn- users-ids-from-org-ids-query [org-ids]
  (-> (sql/select :id)
      (sql/from :users)
      (sql/where [:in :org_id (map str org-ids)])
      (sql-format)))

(defn- users-ids-from-org-ids [tx org-ids]
  (->> org-ids
       users-ids-from-org-ids-query
       (jdbc-query tx)
       (map :id)
       (map str)))

(defn- target-ids [body tx]
  (->> [[]
        (some->> body :emails seq (users-ids-from-emails tx))
        (some->> body :org_ids seq (users-ids-from-org-ids tx))
        (some->> body :ids seq)]
       (apply concat)
       set))

(defn- existing-ids [authentication-system-id tx]
  "returns the current ids of users of a authentication-system,
  authentication-system-id must be an existing id (pkey) of a authentication-system"
  (->> (-> (sql/select :user_id)
           (sql/from :authentication-systems_users)
           (sql/where [:= :authentication-system_id authentication-system-id])
           (sql-format))
       (jdbc-query tx)
       (map :user_id)
       (map str)
       set))

(defn batch-update-users [{tx :tx body :body
                           {authentication-system-id :authentication-system-id} :route-params
                           :as request}]
  (let [target-ids (target-ids body tx)
        existing-ids (existing-ids authentication-system-id tx)
        to-be-removed-ids (set/difference existing-ids target-ids)
        to-be-added-ids (set/difference target-ids existing-ids)]
    (when-not (empty? to-be-removed-ids)
      (->> (-> (sql/delete-from :authentication-systems_users)
               (sql/where [:= :authentication-system_id authentication-system-id])
               (sql/where [:in :user_id to-be-removed-ids])
               (sql-format))
           (jdbc/execute! tx)))
    (when-not (empty? to-be-added-ids)
      (jdbc-insert-multi! tx :authentication-systems_users
                          (->> to-be-added-ids
                               (map (fn [id] {:authentication-system_id authentication-system-id
                                              :user_id id})))))
    {:status 200
     :body {:removed-user-ids to-be-removed-ids
            :added-user-ids to-be-added-ids}}))

;;; put-user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn put-user [{tx :tx :as request
                 body :body
                 {authentication-system-id :authentication-system-id
                  user-id :user-id} :route-params}]
  (utils.jdbc/insert-or-update!
   tx :authentication_systems_users
   ["authentication_system_id = ? AND user_id = ?"
    authentication-system-id user-id]
   (-> body
       (select-keys [:data])
       (merge
        {:authentication_system_id authentication-system-id
         :user_id user-id})))
  {:status 204})

;;; remove-user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-user [{tx :tx :as request
                    {authentication-system-id :authentication-system-id
                     user-id :user-id} :route-params}]
  (if (= 1 (->> ["authentication_system_id = ? AND user_id = ?" authentication-system-id user-id]
                (jdbc-delete! tx :authentication_systems_users)
                ::jdbc/update-count))
    {:status 204}
    (throw (ex-info "Remove authentication-system-user failed" {:request request}))))

;;; user-data ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn user-data [{tx :tx
                  {authentication-system-id :authentication-system-id
                   user-id :user-id} :route-params}]
  (when-let [row (->> (-> (sql/select :*)
                          (sql/from :authentication_systems_users)
                          (sql/where [:= :user_id user-id])
                          (sql/where [:= :authentication-system-id authentication-system-id])
                          sql-format)
                      (jdbc-query tx) first)]
    {:body row}))

;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def authentication-system-user-path
  (path :authentication-system-user {:authentication-system-id ":authentication-system-id" :user-id ":user-id"}))

(def authentication-system-users-path
  (path :authentication-system-users {:authentication-system-id ":authentication-system-id"}))

(defn routes [request]
  (let [handler-key (->> request :uri (match-route paths) :handler)]
    (match [(:request-method request) handler-key]
      [:get :authentication-system-user] (user-data request)
      [:put :authentication-system-user] (put-user request)
      [:delete :authentication-system-user] (remove-user request)
      [:get :authentication-system-users] (users request)
      [:put :authentication-system-users] (batch-update-users request))))

;#### debug ###################################################################

;(debug/debug-ns *ns*)
