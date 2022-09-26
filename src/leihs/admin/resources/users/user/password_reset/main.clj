(ns leihs.admin.resources.users.user.password-reset.main
  (:refer-clojure :exclude [str keyword])
  (:require
    [compojure.core :as cpj]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.users.main :as users]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.random :refer [base32-crockford-rand-str]]
    [logbug.debug :as debug]
    [next.jdbc :as jdbc]
    [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
    [taoensso.timbre :refer [error warn info debug spy]]
    [tick.core :as tick])
  (:import [java.sql Timestamp]
           [java.util UUID]))


(defn token
  ([] (token 6))
  ([n] (base32-crockford-rand-str n)))

(defn timestamp [hrs]
  (assert (int? hrs ))
  (assert (<= hrs (* 7 24)))
  (-> (tick/>>
        (tick/now)
        (tick/new-duration hrs :hours))
      Timestamp/from))

(defn user-by-id [tx-next user-id]
  (some-> (sql/select :email :id)
          (sql/from :users)
          (sql/where [:= :users.id (UUID/fromString user-id)])
          (sql-format {:inline false})
          (->> (jdbc-query tx-next) first)))

(defn create-token
  [tx-next valid-hrs
   {user-email :email user-id :id :as user}]
  (-> (sql/delete-from :user_password_resets)
      (sql/where [:= :user_id user-id])
      (sql-format)
      (->> (jdbc/execute-one! tx-next)))
  (-> (sql/insert-into :user_password_resets)
      (sql/values [{:token (token)
                    :valid_until (timestamp valid-hrs)
                    :user_id user-id
                    :used_user_param user-email}])
      (sql-format :inline false)
      (#(jdbc/execute-one! tx-next % {:return-keys true}))))

(defn create
  [{{user-id :user-id} :route-params
    {valid-hrs :valid_for_hours} :body tx-next :tx-next :as request}]
  (debug {'user-id user-id 'request request})
  (if-let [user (user-by-id tx-next user-id)]
    {:body (create-token tx-next valid-hrs user)}
    (throw (ex-info "taget user not found" {:status 404}))))

(def routes
  (cpj/routes
    (cpj/POST (path :user-password-reset {:user-id ":user-id"}) [] #'create)
    ))


;(debug/debug-ns *ns*)
