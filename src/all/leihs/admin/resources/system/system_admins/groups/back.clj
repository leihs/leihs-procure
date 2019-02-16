(ns leihs.admin.resources.system.system-admins.groups.back
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.sql :as sql]

    [leihs.admin.auth.back :as admin-auth]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.system.system-admins.groups.shared :refer [filter-value]]
    [leihs.admin.resources.groups.back :as groups]
    [leihs.admin.utils.regex :as regex]
    [leihs.admin.utils.jdbc :as utils.jdbc]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [clojure.set :as set]

    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn groups-query [request]
  (let [query (-> request groups/groups-query
                  (sql/merge-left-join :system_admin_groups
                                       [:= :system_admin_groups.group_id :groups.id])
                  (sql/merge-select [:system_admin_groups.group_id :system_admin_group_id]))]
    (if-not (-> request :query-params-raw filter-value)
      query
      (sql/merge-where query [:<> :system_admin_groups.group_id nil]))))


(def system-admin-groups-count-query
  (-> (sql/select :%count.*)
      (sql/from :system_admin_groups)
      (sql/format)))

(defn groups-formated-query [request]
  (-> (groups-query request)
      sql/format))

(defn groups [{tx :tx :as request}]
    {:body
     {:system-admin_groups_count (->> system-admin-groups-count-query
                              (jdbc/query tx)
                              first :count)
      :groups (->> (groups-formated-query request)
                  (jdbc/query tx))}})


;;; update-groups ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn groups-ids-from-emails-query [emails]
  (-> (sql/select :id)
      (sql/from :groups)
      (sql/merge-where [:in :email emails])
      (sql/format)))

(defn- groups-ids-from-emails [tx emails]
  (->> emails
       groups-ids-from-emails-query
       (jdbc/query tx)
       (map :id)
       (map str)))

(defn- groups-ids-from-org-ids-query [org-ids]
  (-> (sql/select :id)
      (sql/from :groups)
      (sql/merge-where [:in :org_id (map str org-ids)])
      (sql/format)))

(defn- groups-ids-from-org-ids [tx org-ids]
  (->> org-ids
       groups-ids-from-org-ids-query
       (jdbc/query tx)
       (map :id)
       (map str)))

(defn- target-ids [body tx]
  (->> [[]
        (some->> body :emails seq (groups-ids-from-emails tx))
        (some->> body :org_ids seq (groups-ids-from-org-ids tx))
        (some->> body :ids seq)]
       (apply concat)
       set))

(defn- existing-ids [system-admin-id tx]
  "returns the current ids of groups of a system-admin,
  system-admin-id must be an existing id (pkey) of a system-admin"
  (->> (-> (sql/select :group_id)
           (sql/from :system-admins_groups)
           (sql/merge-where [:= :system-admin_id system-admin-id])
           (sql/format))
       (jdbc/query tx)
       (map :group_id)
       (map str)
       set))



(defn batch-update-groups [{tx :tx body :body
                           {system-admin-id :system-admin-id} :route-params
                           :as request}]
  (let [target-ids (target-ids body tx)
        existing-ids (existing-ids system-admin-id tx)
        to-be-removed-ids (set/difference existing-ids target-ids)
        to-be-added-ids (set/difference target-ids existing-ids)]
    (logging/info 'target-ids target-ids 'existing-ids existing-ids
                  'to-be-removed-ids to-be-removed-ids
                  'to-be-added-ids to-be-added-ids)
    (when-not (empty? to-be-removed-ids)
      (->> (-> (sql/delete-from :system-admins_groups)
               (sql/merge-where [:= :system-admin_id system-admin-id])
               (sql/merge-where [:in :group_id to-be-removed-ids])
               (sql/format))
           (jdbc/execute! tx)))
    (when-not (empty? to-be-added-ids)
      (jdbc/insert-multi! tx :system-admins_groups
                          (->> to-be-added-ids
                               (map (fn [id] {:system-admin_id system-admin-id
                                              :group_id id})))))
    {:status 200
     :body {:removed-group-ids to-be-removed-ids
            :added-group-ids to-be-added-ids}}))


;;; put-group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn put-group [{tx :tx :as request
                 body :body
                 {group-id :group-id} :route-params}]
  (utils.jdbc/insert-or-update!
    tx :system_admin_groups
    ["group_id = ?" group-id]
    {:group_id group-id})
  {:status 204})


;;; remove-group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-group [{tx :tx :as request
                    {group-id :group-id} :route-params}]
  (if (= 1 (->> ["group_id = ?" group-id]
                (jdbc/delete! tx :system_admin_groups)
                first))
    {:status 204}
    (throw (ex-info "Remove system_admin_groups failed" {:status 409}))))


;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def system-admins-group-path
  (path :system-admins-group {:group-id ":group-id"}))

(def system-admin-groups-path
  (path :system-admin-groups {}))

(def routes
  (-> (cpj/routes
        (cpj/PUT system-admins-group-path [] #'put-group)
        (cpj/DELETE system-admins-group-path [] #'remove-group)
        (cpj/GET system-admin-groups-path [] #'groups)
        (cpj/PUT system-admin-groups-path [] #'batch-update-groups))
      (admin-auth/wrap-authorize  
        {:required-scopes {:scope_admin_read true
                           :scope_admin_write true
                           :scope_system_admin_read true
                           :scope_system_admin_write true}})))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
;(debug/debug-ns 'leihs.admin.resources.system.system-admins.groups.shared)
