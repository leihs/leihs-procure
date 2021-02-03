(ns leihs.admin.resources.users.user.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.core.sql :as sql]
    [leihs.core.auth.core :as auth]

    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.users.choose-core :as choose-core]

    [clojure.set :refer [rename-keys]]
    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [logbug.catcher :as catcher]
    )
  (:import
    [java.awt.image BufferedImage]
    [java.io ByteArrayInputStream ByteArrayOutputStream]
    [java.util Base64]
    [javax.imageio ImageIO]
    ))

;;; data keys ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def user-selects
  [:account_enabled
   :address
   :badge_id
   :city
   :country
   :created_at
   :email
   :extended_info
   :firstname
   :id
   :img256_url
   :img32_url
   :img_digest
   :is_admin
   :lastname
   :login
   :org_id
   :password_sign_in_enabled
   :phone
   :protected
   :secondary_email
   :updated_at
   :url
   :zip
   [(-> (sql/select :%count.*)
        (sql/from :contracts)
        (sql/merge-where [:= :contracts.user_id :users.id]))
    :contracts_count]
   [(-> (sql/select :%count.*)
        (sql/from :access_rights)
        (sql/merge-where [:= :access_rights.user_id :users.id]))
    :inventory_pool_roles_count]])


(def user-write-keys
  [:address
   :account_enabled
   :badge_id
   :city
   :country
   :email
   :extended_info
   :firstname
   :img256_url
   :img32_url
   :img_digest
   :is_admin
   :lastname
   :login
   :org_id
   :password_sign_in_enabled
   :phone
   :protected
   :secondary_email
   :url
   :zip])

(def user-write-keymap
  {})

(def admin-restricted-attributes
  [:is_admin
   :protected])

;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn requester-is-admin? [request]
  (auth/admin-scopes? request))

;;; user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn user-query [user-id]
  (-> (apply sql/select user-selects)
      (sql/from :users)
      (sql/merge-where [:= :id user-id])
      (sql/merge-where [:= :delegator_user_id nil])))

(defn get-user [{tx :tx {user-id :user-id} :route-params}]
  {:body
   (or (->> (-> user-id user-query sql/format)
            (jdbc/query tx) first)
       (throw (ex-info "User not found" {:status 404})))})

;;; delete user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-user [{tx :tx {user-id :user-id} :route-params :as request}]
  (if-let [user (-> request get-user :body)]
    (do (when-not (requester-is-admin? request)
          (when (:protected user)
            (throw (ex-info "Only admins may delete a proteced user. "
                            {:status 403}))))
        (if (= [1] (jdbc/delete! tx :users ["id = ?" user-id]))
          {:status 204}
          (throw (ex-info "Deleted failed" {:status 500}))))
    (throw (ex-info "To be deleted user not found." {:status 404}))))


;;; transfer data ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- transfer-entitlement-groups [tx user-id target-user-id]
  (let [target-entitlemens-ids (->> (-> (sql/select :*)
                                        (sql/from :entitlement_groups_users)
                                        (sql/merge-where [:= :user_id target-user-id])
                                        sql/format)
                                    (jdbc/query tx)
                                    (map :entitlement_group_id))
        source-entitlements-ids (->> (-> (sql/select :*)
                                         (sql/from :entitlement_groups_users)
                                         (sql/merge-where [:= :user_id user-id])
                                         sql/format)
                                     (jdbc/query tx)
                                     (map :entitlement_group_id))
        to-be-transfered-ids (clojure.set/difference (set source-entitlements-ids)
                                                     (set target-entitlemens-ids))]
    (when (seq to-be-transfered-ids)
      (->>
        (-> (sql/update :entitlement_groups_users)
            (sql/set {:user_id target-user-id})
            (sql/merge-where [:= :user_id user-id])
            (sql/merge-where [:in :entitlement_group_id to-be-transfered-ids])
            sql/format)
        (jdbc/execute! tx)))
    (->>
       (-> (sql/delete-from :entitlement_groups_users)
           (sql/merge-where [:= :user_id user-id])
           sql/format)
       (jdbc/execute! tx))))

(defn transfer-data [user-id target-user-id tx]
  (doseq [[table fields] [[:audited_requests [:user_id]]
                          [:contracts [:user_id]]
                          [:customer_orders [:user_id]]
                          [:orders [:user_id]]
                          [:reservations [:user_id :handed_over_by_user_id :returned_to_user_id :delegated_user_id]]]]
    (doseq [field fields]
      (jdbc/update! tx table
                    {(str field) target-user-id}
                    [(str field " = ?") user-id])))
  (transfer-entitlement-groups tx user-id target-user-id))

(defn transfer-data-and-delete-user
  [{{user-id :user-id target-user-uid :target-user-uid} :route-params
    tx :tx :as request}]
  (let [target-user-id (-> target-user-uid (choose-core/find-user-by-some-uid! tx) :id)
        del-user (->> user-id
                      user-query
                      sql/format
                      (jdbc/query tx)
                      first)]
    (when-not del-user
      (throw (ex-info "To be deleted user not found." {:status 404})))
    (when-not (requester-is-admin? request)
      (when (:protected del-user)
        (throw (ex-info "Only admins may delete a proteced user. "
                        {:status 403}))))
    (transfer-data user-id target-user-id tx)
    (if (= [1] (jdbc/delete! tx :users ["id = ?" user-id]))
      {:status 204}
      (throw (ex-info "Deleteing user failed." {:status 500})))))


;;; image handling ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def IMG-DATA-URL-PREFIX "data:image/jpeg;base64")

(defn assert-proper-image-type! [prefix]
  (when-not (= IMG-DATA-URL-PREFIX prefix)
    (throw (ex-info "Image is not of expected type 'data:image/jpeg;base64'!"
                    {:status 422
                     :body (str "The first chars of img256_url must be equal to data:image/jpeg;base64. "
                                "See also https://tools.ietf.org/html/rfc2397.")}))))

(defn data-url-img->buffered-image ^BufferedImage [data-url-img]
  (let [[img-type img-s] (clojure.string/split data-url-img #"," 2)
        img-ba (.decode (Base64/getDecoder) (.getBytes img-s "UTF-8"))
        in (ByteArrayInputStream. img-ba)]
    (assert-proper-image-type! img-type)
    (ImageIO/read in)))

(defn buffered-image->data-url-img ^String [^BufferedImage img]
  (let [os (ByteArrayOutputStream.)
        _ (ImageIO/write img "jpg" os)
        ba (.toByteArray os)
        base64 (.encodeToString (Base64/getEncoder) ba)]
    (clojure.string/join "," [IMG-DATA-URL-PREFIX base64])))

(defn resized-img ^BufferedImage [^BufferedImage img ^Integer dim]
  (let [img-buffer (BufferedImage. dim dim (.getType img))
        graphics (.createGraphics img-buffer)]
    (.drawImage graphics img 0 0 dim dim nil)
    (.dispose graphics)
    img-buffer))

(defn remove-images [data]
  (assoc data
         :img256_url nil
         :img32_url nil))

(defn set-images [data img-data-url]
  (let [img (data-url-img->buffered-image img-data-url)
        img256-data-url (-> img (resized-img 256) buffered-image->data-url-img)
        img32-data-url (-> img (resized-img 32) buffered-image->data-url-img)]
    (assoc data
           :img256_url img256-data-url
           :img32_url img32-data-url)))

(defn process-images [data]
  (if-let [img-data-url (-> data :img256_url presence)]
    (set-images data img-data-url)
    (if (and (contains? data :img256_url)
             (not (-> data :img256_url presence)))
      (remove-images data)
      data)))


;;; password ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn password-hash
  ([password tx]
   (->> ["SELECT crypt(?,gen_salt('bf',10)) AS pw_hash" password]
        (jdbc/query tx)
        first :pw_hash)))

(defn insert-pw-hash [data tx]
  (if-let [password (-> data :password presence)]
    (assoc data :pw_hash (password-hash password tx))
    data))


;;; update user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defn prepare-write-data [data]
  (-> data
      (select-keys user-write-keys)
      (rename-keys user-write-keymap)))

(defn check-protected-attributes-do-not-change! [data user]
  (when-let [attr (some
                    #(and (contains? data %)
                          (not= (get user %) (get data %)))
                    admin-restricted-attributes)]
    (throw (ex-info "Only admins may change restricted-attributes"
                    {:status 403 :attribure attr}))))

(defn patch-user
  ([{tx :tx data :body {user-id :user-id} :route-params :as request}]
   (patch-user user-id (prepare-write-data data) tx request))
  ([user-id data tx request]
   (if-let [user (-> request get-user :body)]
     (do (when-not (requester-is-admin? request)
           (check-protected-attributes-do-not-change! data user)
           (when (-> user :protected not false?)
             (throw (ex-info "Only admins may update protected groups"
                             {:status 403}))))
         (logging/info 'data data)
         (or (= [1] (jdbc/update! tx :users data ["id = ?" user-id]))
             (throw (ex-info "Number of updated rows does not equal one." {} )))
         (get-user request))
     {:status 404})))


;;; create user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn check-protected-attributes-are-not-set! [data]
  (when-let [attr (some
                    #(true? (get data %))
                    admin-restricted-attributes)]
    (throw (ex-info "Only admins can set admin restricted-attributes"
                    {:status 403 :attribure attr}))))

(defn create-user
  ([{tx :tx data :body :as request}]
   (create-user (prepare-write-data data) tx request))
  ([data tx request]
   (when-not (requester-is-admin? request)
     (check-protected-attributes-are-not-set! data))
   (if-let [user (first (jdbc/insert! tx :users data))]
     {:body user}
     {:status 422
      :body "No user has been created."})))

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def user-path (path :user {:user-id ":user-id"}))

(def user-transfer-path
  (path :user-transfer-data {:user-id ":user-id"
                             :target-user-uid ":target-user-uid"}))

(def routes
  (cpj/routes
    (cpj/GET user-path [] #'get-user)
    (cpj/PATCH user-path [] #'patch-user)
    (cpj/DELETE user-path [] #'delete-user)
    ;(cpj/POST user-transfer-path [] #'transfer-data)
    (cpj/DELETE user-transfer-path [] #'transfer-data-and-delete-user)
    (cpj/POST (path :users) [] #'create-user)))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
