(ns leihs.admin.resources.groups.group.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [bidi.bidi :refer [match-route]]
   [clojure.core.match :refer [match]]
   [clojure.set :refer [rename-keys]]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.common.users-and-groups.core :as users-and-groups]
   [leihs.admin.paths :refer [path paths]]
   [leihs.core.auth.core :as auth]
   [logbug.catcher :as catcher]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [delete! insert! query update!] :rename {query jdbc-query,
                                                                  insert! jdbc-insert!,
                                                                  update! jdbc-update!,
                                                                  delete! jdbc-delete!}]))

(def admin-restricted-attributes
  [:admin_protected
   :org_id
   :organization])

(def system-admin-restricted-attributes
  [:system_admin_protected])

;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn requester-is-admin? [request]
  (auth/admin-scopes? request))

(defn requester-is-system-admin? [request]
  (auth/system-admin-scopes? request))

;;; data keys ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def group-selects
  [:groups.id
   :name
   :description
   :organization
   :org_id
   :system_admin_protected
   :admin_protected
   [(-> (sql/select :%count.*)
        (sql/from :groups_users)
        (sql/where [:= :groups_users.group_id :groups.id]))
    :users_count]
   [(-> (sql/select :%count.*)
        (sql/from :group_access_rights)
        (sql/where [:= :groups.id :group_access_rights.group_id]))
    :inventory_pools_roles_count]
   :created_at
   :updated_at])

(def group-write-keys
  [:name
   :admin_protected
   :description
   :organization
   :org_id
   :system_admin_protected])

(def group-write-keymap
  {})

;;; group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn group-query [group-id]
  (-> (apply sql/select group-selects)
      (sql/from :groups)
      (sql/where [:= :id group-id])))

(defn get-group [{tx :tx-next {group-id :group-id} :route-params}]
  {:body (->> group-id group-query sql-format (jdbc-query tx) first)})

;;; delete group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-group [{tx :tx-next {group-id :group-id} :route-params :as request}]
  (if-let [group (-> request get-group :body)]
    (do (when-not (requester-is-admin? request)
          (users-and-groups/assert-not-admin-proteced! group))
        (when-not (requester-is-system-admin? request)
          (users-and-groups/assert-not-system-admin-proteced! group))
        (if (= 1 (::jdbc/update-count
                  (jdbc-delete! tx :groups ["id = ?" group-id])))
          {:status 204}
          (throw (ex-info "Deleted failed" {:status 500}))))
    (throw (ex-info "To be deleted group not found." {:status 404}))))

;;; update group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn prepare-write-data [data tx]
  (catcher/with-logging
    {}
    (-> data
        (select-keys group-write-keys)
        (rename-keys group-write-keymap))))

(defn protect-admin! [data group]
  (users-and-groups/assert-attributes-do-not-change!
   data group admin-restricted-attributes)
  (users-and-groups/assert-not-admin-proteced! group))

(defn protect-system-admin! [data group]
  (users-and-groups/assert-attributes-do-not-change!
   data group system-admin-restricted-attributes)
  (users-and-groups/assert-not-system-admin-proteced! group))

(defn patch-group
  ([{tx :tx-next data :body {group-id :group-id} :route-params :as request}]
   (patch-group group-id (prepare-write-data data tx) tx request))
  ([group-id data tx request]
   (if-let [group (-> request get-group :body)]
     (do (users-and-groups/protect-leihs-core! group)
         (users-and-groups/protect-leihs-core! data)
         (when-not (requester-is-admin? request)
           (protect-admin! data group))
         (when-not (requester-is-system-admin? request)
           (protect-system-admin! data group))
         (or (= 1 (::jdbc/update-count
                   (jdbc-update! tx :groups data ["id = ?" group-id])))
             (throw (ex-info "Number of updated rows does not equal one." {})))
         (-> (get-group request) (assoc :status 200)))
     {:status 404})))

;;; create group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-group
  ([{tx :tx-next data :body :as request}]
   (create-group (prepare-write-data data tx) tx request))
  ([data tx request]
   (users-and-groups/protect-leihs-core! data)
   (when-not (requester-is-admin? request)
     (users-and-groups/assert-attributes-are-not-set!
      data admin-restricted-attributes))
   (when-not (requester-is-system-admin? request)
     (users-and-groups/assert-attributes-are-not-set!
      data system-admin-restricted-attributes))
   (if-let [group (jdbc-insert! tx :groups data)]
     {:status 201, :body group}
     (throw (ex-info "Group has not been created" {:status 534})))))

;;; roles ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn group-inventory-pools-roles-query [group-id]
  (-> (sql/select :iprs.* [:inventory_pools.name :inventory_pool_name])
      (sql/from [:group_access_rights :iprs])
      (sql/where [:= :group_id group-id])
      (sql/join :inventory_pools [:= :iprs.inventory_pool_id :inventory_pools.id])
      sql-format))

(defn inventory-pools-roles [group-id tx]
  (->> group-id
       group-inventory-pools-roles-query
       (jdbc-query tx)))

(defn group-inventory-pools-roles
  [{tx :tx-next data :body {group-id :group-id} :route-params}]
  {:body
   {:inventory_pools_roles
    (inventory-pools-roles group-id tx)}})

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def group-path (path :group {:group-id ":group-id"}))

(def group-transfer-path
  (path :group-transfer-data {:group-id ":group-id"
                              :target-group-id ":target-group-id"}))

(defn routes [request]
  (let [handler-key (->> request :uri (match-route paths) :handler)]
    (match [(:request-method request) handler-key]
      [:get :group] (get-group request)
      [:get :group-inventory-pools-roles] (group-inventory-pools-roles request)
      [:patch :group] (patch-group request)
      [:delete :group] (delete-group request)
      [:post :groups] (create-group request))))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)

;(debug/debug-ns *ns*)
