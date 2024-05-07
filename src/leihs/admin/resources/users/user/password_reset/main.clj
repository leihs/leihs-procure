(ns leihs.admin.resources.users.user.password-reset.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.random :refer [base32-crockford-rand-str]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
   [taoensso.timbre :refer [debug]]
   [tick.core :as tick])
  (:import
   [java.sql Timestamp]
   [java.util UUID]))

(defn token
  ([] (token 6))
  ([n] (base32-crockford-rand-str n)))

(defn timestamp [hrs]
  (assert (int? hrs))
  (assert (<= hrs (* 7 24)))
  (-> (tick/>>
       (tick/now)
       (tick/new-duration hrs :hours))
      Timestamp/from))

(defn user-by-id [tx user-id]
  (some-> (sql/select :email :id)
          (sql/from :users)
          (sql/where [:= :users.id user-id])
          sql-format
          (->> (jdbc-query tx) first)))

(defn create-token
  [tx valid-hrs
   {user-email :email user-id :id :as user}]
  (-> (sql/delete-from :user_password_resets)
      (sql/where [:= :user_id user-id])
      (sql-format)
      (->> (jdbc/execute-one! tx)))
  (-> (sql/insert-into :user_password_resets)
      (sql/values [{:token (token)
                    :valid_until (timestamp valid-hrs)
                    :user_id user-id
                    :used_user_param user-email}])
      (sql-format :inline false)
      (#(jdbc/execute-one! tx % {:return-keys true}))))

(defn create
  [{{user-id :user-id} :route-params
    {valid-hrs :valid_for_hours} :body tx :tx :as request}]
  (debug {'user-id user-id 'request request})
  (if-let [user (user-by-id tx user-id)]
    {:status 201, :body (create-token tx valid-hrs user)}
    (throw (ex-info "taget user not found" {:status 404}))))

(defn routes [request]
  (case (:request-method request)
    :post (create request)))

;(debug/debug-ns *ns*)
