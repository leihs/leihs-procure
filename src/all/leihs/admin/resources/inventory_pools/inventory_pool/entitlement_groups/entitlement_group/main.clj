(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.shared :refer [normalized-inventory-pool-id!]]
    [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.main :as entitlement-groups]
    [leihs.admin.resources.groups.main :as groups]
    [leihs.admin.utils.regex :as regex]
    [leihs.core.sql :as sql]
    [leihs.admin.utils.jdbc :as utils.jdbc]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [clojure.set :as set]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]))

(defn query [inventory-pool-id entitlement-group-id]
  (-> entitlement-groups/base-query
      (sql/merge-where
        [:= :entitlement_groups.inventory_pool_id inventory-pool-id])
      (sql/merge-where [:= :entitlement_groups.id entitlement-group-id])))

(defn entitlement-group
  [{{inventory-pool-id :inventory-pool-id
     entitlement-group-id :entitlement-group-id} :route-params
    tx :tx :as request}]
  (if-let [eg (->> (query inventory-pool-id entitlement-group-id)
                   sql/format
                   (jdbc/query tx)
                   first)]
    {:body eg}
    {:status 404}))

(def routes
  (cpj/routes
    (cpj/GET (path :inventory-pool-entitlement-group
                   {:inventory-pool-id ":inventory-pool-id"
                    :entitlement-group-id ":entitlement-group-id"
                    }) [] #'entitlement-group)))

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/wrap-with-log-debug #'filter-suspended)
;(debug/wrap-with-log-debug #'groups-formated-query)
;(debug/debug-ns *ns*)
