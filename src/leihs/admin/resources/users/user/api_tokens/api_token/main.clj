(ns leihs.admin.resources.users.user.api-tokens.api-token.main
  (:require [crypto.random]
            [honey.sql :refer [format] :rename {format sql-format}]
            [honey.sql.helpers :as sql]
            [leihs.core.core :refer [str]]
            [next.jdbc :as jdbc])
  (:import (com.google.common.io BaseEncoding)
           (java.time OffsetDateTime)
           (org.joda.time DateTime)))

(def api-token-selects
  [:created_at
   :description
   :expires_at
   :id
   :scope_read
   :scope_write
   :scope_admin_read
   :scope_admin_write
   :scope_system_admin_read
   :scope_system_admin_write
   :token_part
   :updated_at
   :user_id])

(def allowed-insert-and-patch-keys
  [:description
   :expires_at
   :scope_admin_read
   :scope_admin_write
   :scope_system_admin_read
   :scope_system_admin_write
   :scope_read
   :scope_write])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def b32 (BaseEncoding/base32))

(defn secret [n]
  (->> n crypto.random/bytes
       (.encode b32)
       (map char)
       (apply str)))

(defn token-hash
  ([password tx]
   (-> (sql/select [[:crypt password [:gen_salt "bf"]] :token_hash])
       sql-format
       (->> (jdbc/execute-one! tx))
       :token_hash)))

(defn insert-token [params tx]
  (let [insert-sql (-> (sql/insert-into :api_tokens)
                       (sql/values [params])
                       (sql/returning :*))]
    (jdbc/execute-one! tx (sql-format insert-sql))))

(defn parse-and-cast-uuid [s]
  (if (= (type s) java.util.UUID)
    s
    (java.util.UUID/fromString s)))

(defn parse-iso8601 [s]
  (let [joda_datetime (DateTime. (* 1000 (.toEpochSecond (OffsetDateTime/parse s))))
        millis (.getMillis joda_datetime)
        sql-timestamp (java.sql.Timestamp. millis)]
    sql-timestamp))

(defn normalize-create-or-update-params [params]
  (->> params
       (map (fn [[k v]]
              (case k
                :expires_at [k (parse-iso8601 v)]
                :updated_at [k (parse-iso8601 v)]
                :user_id [k (parse-and-cast-uuid v)]
                [k v])))
       (into {})))

;;; post ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create_user-api-token
  ([{body :body tx :tx {user-id :user-id} :route-params}]
   (create_user-api-token user-id body tx))
  ([user-id body tx]
   (let [token-secret (secret 20)
         token-hash (token-hash token-secret tx)
         params (-> body
                    (select-keys allowed-insert-and-patch-keys)
                    (assoc :token_hash token-hash
                           :token_part (subs token-secret 0 5)
                           :user_id user-id)
                    normalize-create-or-update-params)
         token (insert-token params tx)]
     {:status 200
      :body (assoc token :token_secret token-secret)})))

;;; patch ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch-api-token
  ([{tx :tx data :body {user-id :user-id
                        api-token-id :api-token-id} :route-params}]
   (patch-api-token api-token-id user-id data tx))
  ([api-token-id user-id data tx]
   (let [user-token-condition [:and [:= :user_id [:cast user-id :uuid]]
                               [:= :id [:cast api-token-id :uuid]]]]
     (when (-> (sql/select :*)
               (sql/from :api_tokens)
               (sql/where user-token-condition)
               sql-format
               (->> (jdbc/execute-one! tx)))
       (let [revised-data (-> data
                              (normalize-create-or-update-params)
                              (dissoc :id :created_at))]
         (-> (sql/update :api_tokens)
             (sql/set revised-data)
             (sql/where user-token-condition)
             sql-format
             (->> (jdbc/execute-one! tx))))
       {:status 204}))))

;;; get ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn api-token-query [api-token-id user-id]
  (-> (apply sql/select api-token-selects)
      (sql/from :api_tokens)
      (sql/where [:and [:= :user_id [:cast user-id :uuid]]] [:= :id [:cast api-token-id :uuid]])
      sql-format))

(defn get-api-token
  ([{tx :tx {user-id :user-id api-token-id :api-token-id} :route-params}]
   (get-api-token api-token-id user-id tx))
  ([api-token-id user-id tx]
   (when-let [api-token (->> (api-token-query api-token-id user-id)
                             (jdbc/execute-one! tx))]
     {:body api-token})))

;;; delete ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-api-token [{tx :tx {user-id :user-id
                                 api-token-id :api-token-id} :route-params}]
  (let [result (-> (sql/delete-from :api-tokens)
                   (sql/where [:and [:= :user_id [:cast user-id :uuid]] [:= :id [:cast api-token-id :uuid]]])
                   sql-format
                   (->> (jdbc/execute-one! tx)))
        delete-result-count (->> result :next.jdbc/update-count)]
    (if (= 1 delete-result-count)
      {:status 204}
      {:status 404 :body "Delete api-token-id failed without error."})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [request]
  (case (:request-method request)
    :get (get-api-token request)
    :patch (patch-api-token request)
    :delete (delete-api-token request)))