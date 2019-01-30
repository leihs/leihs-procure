(ns leihs.admin.resources.system.authentication-system.groups.back
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.sql :as sql]

    [leihs.admin.auth.back :as admin-auth]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.system.authentication-system.groups.shared :refer [filter-value]]
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


(defn groups-query
  [{{authentication-system-id :authentication-system-id} :route-params
    :as request}]
  (let [query (-> request groups/groups-query
                  (sql/merge-left-join :authentication_systems_groups
                                       [:and
                                        [:= :authentication_systems_groups.group_id :groups.id]
                                        [:= :authentication_systems_groups.authentication_system_id authentication-system-id]])
                  (sql/merge-select [:authentication_systems_groups.group_id :authentication_system_group_id]))]
    (if-not (-> request :query-params-raw filter-value)
      query
      (sql/merge-where query [:<> :authentication_systems_groups.group_id nil]))))

(defn groups-formated-query [request]
  (-> (groups-query request)
      sql/format))

(defn authentication-system-groups-count-query
  [{{authentication-system-id :authentication-system-id} :route-params
    :as request}]
  (-> (sql/select :%count.*)
      (sql/from :authentication_systems_groups)
      (sql/merge-where [:= authentication-system-id
                        :authentication_systems_groups.authentication_system_id])
      sql/format))

(defn groups [{tx :tx :as request}]
  {:body
   {:groups_count (->> request
                       authentication-system-groups-count-query
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
           (sql/from :authentication-system_groups)
           (sql/merge-where [:= :system-admin_id system-admin-id])
           (sql/format))
       (jdbc/query tx)
       (map :group_id)
       (map str)
       set))




;;; put-group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn put-group [{tx :tx :as request
                  body :body
                  {authentication-system-id :authentication-system-id
                   group-id :group-id} :route-params}]
  (utils.jdbc/insert-or-update!
    tx :authentication_systems_groups
    ["authentication_system_id = ? AND group_id = ?" authentication-system-id group-id]
    {:authentication_system_id authentication-system-id :group_id group-id})
  {:status 204})


;;; remove-group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-group [{tx :tx :as request
                    {group-id :group-id
                     authentication-system-id :authentication-system-id} :route-params}]
  (if (= 1 (->> ["group_id = ? AND authentication_system_id = ?"
                 group-id authentication-system-id]
                (jdbc/delete! tx :authentication_systems_groups)
                first))
    {:status 204}
    (throw (ex-info "Remove authentication_systems_groups failed" {:status 409}))))


;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def authentication-system-group-path
  (path :authentication-system-group
        {:authentication-system-id ":authentication-system-id"
         :group-id ":group-id"}))

(def authentication-system-groups-path
  (path :authentication-system-groups
        {:authentication-system-id ":authentication-system-id" }))

(def routes
  (-> (cpj/routes
        (cpj/PUT authentication-system-group-path [] #'put-group)
        (cpj/DELETE authentication-system-group-path [] #'remove-group)
        (cpj/GET authentication-system-groups-path [] #'groups))
      (admin-auth/wrap-authorize #{} {:scope_admin_read true
                                      :scope_admin_write true
                                      :scope_system_admin_read true
                                      :scope_system_admin_write true})))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
;(debug/debug-ns 'leihs.admin.resources.system.authentication-system.groups.shared)
