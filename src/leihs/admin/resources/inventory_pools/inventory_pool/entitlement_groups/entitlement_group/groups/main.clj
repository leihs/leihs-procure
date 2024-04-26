(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement_groups.entitlement_group.groups.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.common.membership.groups.main :refer [extend-with-membership]]
   [leihs.admin.resources.groups.main :as groups]
   [leihs.admin.utils.jdbc :as utils.jdbc]
   [leihs.admin.utils.seq :as seq]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [delete! query] :rename {query jdbc-query, delete! jdbc-delete!}]
   [taoensso.timbre :refer [debug]]))

;;; groups ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn member-expr [entitlement-group-id]
  [:exists
   (-> (sql/select true)
       (sql/from :entitlement_groups_groups)
       (sql/where [:= :groups.id :entitlement_groups_groups.group_id])
       (sql/where [:= :entitlement_groups_groups.entitlement_group_id entitlement-group-id]))])

(defn groups-query
  [{{entitlement-group-id :entitlement-group-id} :route-params :as request}]
  (debug entitlement-group-id)
  (-> (groups/groups-query request)
      (extend-with-membership  (member-expr entitlement-group-id) request)
      (sql/select [entitlement-group-id :entitlement_group_id])))

(defn groups-formated-query [request]
  (-> request groups-query sql-format))

(defn groups [{tx :tx-next :as request}]
  (let [query (groups-query request)
        offset (:offset query)]
    {:body
     {:groups (-> query sql-format
                  (->> (jdbc-query tx)
                       (seq/with-index offset)
                       seq/with-page-index))}}))

;;; add ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-entitlement-group-group
  [{{inventory-pool-id :inventory-pool-id
     entitlement-group-id :entitlement-group-id
     group-id :group-id} :route-params
    tx :tx-next :as request}]
  (utils.jdbc/insert-or-update!
   tx :entitlement_groups_groups
   ["entitlement_group_id = ? AND group_id = ?  " entitlement-group-id group-id]
   {:entitlement_group_id entitlement-group-id :group_id group-id})
  {:status 204})

;;; remove ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-entitlement-group-group
  [{{inventory-pool-id :inventory-pool-id
     entitlement-group-id :entitlement-group-id
     group-id :group-id} :route-params
    tx :tx-next :as request}]
  (if (= 1 (::jdbc/update-count
            (jdbc-delete! tx :entitlement_groups_groups
                          ["entitlement_group_id = ? AND group_id = ?
                           " entitlement-group-id group-id])))
    {:status 204}
    {:status 404 :body "Remove entitlement group failed without error."}))

;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [request]
  (case (:request-method request)
    :get (groups request)
    :put (add-entitlement-group-group request)
    :delete (remove-entitlement-group-group request)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'filter-suspended)
;(debug/wrap-with-log-debug #'groups-formated-query)
;(debug/debug-ns *ns*)
