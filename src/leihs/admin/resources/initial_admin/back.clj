(ns leihs.admin.resources.initial-admin.back
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.paths :refer [path]]
   [leihs.core.auth.shared :refer [password-hash]]
   [leihs.core.constants :refer [PASSWORD_AUTHENTICATION_SYSTEM_ID]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [redirect]]))

(defn set-password-sql [user-id pw-hash]
  (-> (sql/insert-into :authentication_systems_users)
      (sql/values [{:user_id [:cast user-id :uuid]
                    :authentication_system_id PASSWORD_AUTHENTICATION_SYSTEM_ID
                    :data pw-hash}])
      (sql/on-conflict :user_id :authentication_system_id)
      (sql/do-update-set :data {:raw "EXCLUDED.data"})
      (sql/returning :*)
      sql-format))

(defn set-password [user-id password tx]
  (let [pw-hash (password-hash password tx)
        sql-command (set-password-sql user-id pw-hash)
        result (jdbc/execute! tx sql-command {:return-keys true})]
    {:body result}))

(defn some-admin? [tx]
  (->> ["SELECT true AS has_admin FROM users WHERE is_admin = true"]
       (jdbc/execute-one! tx)
       (:has_admin)
       boolean))

(defn prepare-data [data]
  (-> data
      (select-keys [:email])
      (assoc :is_admin true)
      (assoc :admin_protected true)
      (assoc :is_system_admin true)
      (assoc :system_admin_protected true)
      (assoc :lastname "Admin")
      (assoc :firstname "Initial")))

(defn insert-user [data tx]
  (let [query (-> (sql/insert-into :users)
                  (sql/values [data])
                  (sql/returning :*)
                  (sql-format))]
    (jdbc/execute-one! tx query)))

(defn make-procurement-admin [{user-id :id} tx]
  (-> (sql/insert-into :procurement_admins)
      (sql/values [{:user_id user-id}])
      (sql/returning :*)
      sql-format
      (->> (jdbc/execute-one! tx))))

(defn create-initial-admin
  ([{tx :tx form-params :form-params data :body}]
   (create-initial-admin (if (empty? form-params)
                           data form-params) tx))
  ([data tx]
   (if (some-admin? tx)
     {:status 403
      :body "An admin user already exists!"}
     (let [user (-> data prepare-data (insert-user tx))]
       (assert user)
       (assert (set-password (:id user)
                             (:password data)
                             tx))
       (assert (make-procurement-admin user tx))
       {:status 201
        :body "Added initial admin user!"}))))

(defn routes [request]
  (case (:request-method request)
    :post (create-initial-admin request)))

;#### debug ###################################################################
;(debug/debug-ns 'cider-ci.utils.shutdown)
;(debug/debug-ns *ns*)
