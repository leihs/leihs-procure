(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.groups.main
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.sql :as sql]

    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.groups.main :as groups]
    [leihs.admin.common.membership.groups.main :refer [extend-with-membership]]
    [leihs.admin.utils.jdbc :as utils.jdbc]
    [leihs.admin.utils.seq :as seq]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [clojure.set :as set]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]))

(defn member-expr [delegation-id]
  [:exists
   (-> (sql/select true)
       (sql/from [:delegations_groups :dgs])
       (sql/merge-where [:= :groups.id :dgs.group_id])
       (sql/merge-where [:= :dgs.delegation_id delegation-id]))])

(defn groups-query
  [{{delegation-id :delegation-id} :route-params :as request}]
  (-> (groups/groups-query request)
      (extend-with-membership  (member-expr delegation-id) request)))

(defn groups [{tx :tx :as request}]
  (let [query (groups-query request)
        offset (:offset query) ]
    {:body
     {:groups (-> query sql/format
                  (->>
                    (jdbc/query tx)
                    (seq/with-index offset)
                    seq/with-page-index))}}))

;;; add ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-group
  [{{delegation-id :delegation-id
     group-id :group-id} :route-params
    tx :tx :as request}]
  (utils.jdbc/insert-or-update!
    tx :delegations_groups
    ["delegation_id = ? AND group_id = ?  " delegation-id group-id]
    {:delegation_id delegation-id :group_id group-id}))


;;; remove ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-group
  [{{delegation-id :delegation-id
     group-id :group-id} :route-params
    tx :tx :as request }]
  (if (= [1] (jdbc/delete! tx :delegations_groups
                           ["delegation_id= ? AND group_id = ?
                            " delegation-id group-id]))
    {:status 204}
    {:status 404 :body "Remove group failed without error."}))


;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def groups-path
  (path :inventory-pool-delegation-groups
        {:inventory-pool-id ":inventory-pool-id"
         :delegation-id ":delegation-id"}))

(def group-path
  (path :inventory-pool-delegation-group
        {:inventory-pool-id ":inventory-pool-id"
         :delegation-id ":delegation-id"
         :group-id ":group-id"}))

(def routes
  (cpj/routes
    (cpj/GET groups-path _ #'groups)
    (cpj/DELETE group-path _ #'remove-group)
    (cpj/PUT group-path _ #'add-group)))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/wrap-with-log-debug #'groups-formated-query)
;(debug/debug-ns *ns*)
