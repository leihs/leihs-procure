(ns leihs.admin.resources.users.user.api-tokens.main
  (:require [honey.sql :refer [format] :rename {format sql-format}]
            [honey.sql.helpers :as sql]
            [leihs.admin.resources.users.user.api-tokens.api-token.main :refer [create_user-api-token]]
            [next.jdbc :as jdbc]))

(defn user-api-tokens-query [user-id]
  (-> (sql/select :id :description :token_part :scope_read :scope_write :scope_admin_read :scope_admin_write :expires_at :created_at)
      (sql/from :api_tokens)
      (sql/where [:= :api_tokens.user_id [:cast user-id :uuid]])
      (sql/order-by [:created_at :desc])
      sql-format))

(defn user-api-tokens
  ([{tx :tx {user-id :user-id} :route-params}]
   (user-api-tokens user-id tx))
  ([user-id tx]
   {:body
    {:user-api-tokens
     (jdbc/execute! tx (user-api-tokens-query user-id))}}))

(defn routes [request]
  (case (:request-method request)
    :get (user-api-tokens request)
    :post (create_user-api-token request)))
