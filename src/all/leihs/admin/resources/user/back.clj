(ns leihs.admin.resources.user.back
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.core.sql :as sql]

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
   :url
   :zip])

(def user-write-keymap
  {})


;;; user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn user-query [user-id]
  (-> (apply sql/select user-selects)
      (sql/from :users)
      (sql/merge-where [:= :id user-id])
      sql/format))

(defn user [{tx :tx {user-id :user-id} :route-params}]
  {:body
   (first (jdbc/query tx (user-query user-id)))})

;;; delete user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-user [{tx :tx {user-id :user-id} :route-params}]
  (assert user-id)
  (if (= [1] (jdbc/delete! tx :users ["id = ?" user-id]))
    {:status 204}
    {:status 404 :body "Delete user failed without error."}))

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

(defn transfer-data
  ([{tx :tx {user-id :user-id target-user-id :target-user-id} :route-params}]
   (transfer-data user-id target-user-id tx))
  ([user-id target-user-id tx]
   (doseq [[table fields] [[:audited_requests [:user_id]]
                           [:contracts [:user_id]]
                           [:orders [:user_id]]
                           [:reservations [:user_id :handed_over_by_user_id :returned_to_user_id :delegated_user_id]]]]
     (doseq [field fields]
       (jdbc/update! tx table
                     {(str field) target-user-id}
                     [(str field " = ?") user-id])))
   (transfer-entitlement-groups tx user-id target-user-id)
   {:status 204}))

(defn transfer-data-and-delete-user
  ([{tx :tx {user-id :user-id target-user-id :target-user-id} :route-params}]
   (transfer-data-and-delete-user user-id target-user-id tx))
  ([user-id target-user-id tx]
   (transfer-data user-id target-user-id tx)
   (if (= [1] (jdbc/delete! tx :users ["id = ?" user-id]))
     {:status 204}
     {:status 404 :body "Delete user failed without error."})))


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

(defn prepare-write-data [data tx]
  (catcher/with-logging
    {}
    (-> data
        (select-keys user-write-keys)
        (rename-keys user-write-keymap))))

(defn patch-user
  ([{tx :tx data :body {user-id :user-id} :route-params}]
   (patch-user user-id (prepare-write-data data tx) tx))
  ([user-id data tx]
   (when (->> ["SELECT true AS exists FROM users WHERE id = ?" user-id]
              (jdbc/query tx )
              first :exists)
     (or (= [1] (jdbc/update! tx :users data ["id = ?" user-id]))
         (throw (ex-info "Number of updated rows does not equal one." {} )))
     {:body
      (first (jdbc/query tx (user-query user-id)))})))


;;; create user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn user-inventory-pools-roles-query [user-id]
  (-> (sql/select :iprs.* [:inventory_pools.name :inventory_pool_name])
      (sql/from [:access_rights :iprs])
      (sql/merge-where [:= :user_id user-id])
      (sql/merge-where [:or [:= nil :suspended_until] [:> :suspended_until (sql/raw "now()")]])
      (sql/merge-join :inventory_pools [:= :iprs.inventory_pool_id :inventory_pools.id])
      sql/format))

(defn inventory-pools-roles [user-id tx]
  (->> user-id
       user-inventory-pools-roles-query
       (jdbc/query tx)))

(defn user-inventory-pools-roles
  [{tx :tx data :body {user-id :user-id} :route-params}]
  {:body
   {:inventory_pools_roles
    (inventory-pools-roles user-id tx)
    }})


;;; create user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-user
  ([{tx :tx data :body}]
   (create-user (prepare-write-data data tx) tx))
  ([data tx]
   (if-let [user (first (jdbc/insert! tx :users data))]
     {:body user}
     {:status 422
      :body "No user has been created."})))

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def user-path (path :user {:user-id ":user-id"}))

(def user-transfer-path
  (path :user-transfer-data {:user-id ":user-id"
                             :target-user-id ":target-user-id"}))

(def routes
  (cpj/routes
    (cpj/GET user-path [] #'user)
    (cpj/GET (path :user-inventory-pools-roles {:user-id ":user-id"}) [] #'user-inventory-pools-roles)
    (cpj/PATCH user-path [] #'patch-user)
    (cpj/DELETE user-path [] #'delete-user)
    (cpj/POST user-transfer-path [] #'transfer-data)
    (cpj/DELETE user-transfer-path [] #'transfer-data-and-delete-user)
    (cpj/POST (path :users) [] #'create-user)))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
