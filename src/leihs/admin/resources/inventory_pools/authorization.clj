(ns leihs.admin.resources.inventory-pools.authorization
  (:refer-clojure :exclude [str keyword])
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.set :refer [rename-keys]]
    [compojure.core :as cpj]
    [leihs.admin.common.roles.core :as roles]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.inventory-pools.shared :as shared :refer [inventory-pool-path]]
    [leihs.core.auth.core :refer [http-safe?]]
    [leihs.core.core :refer [keyword str presence]]
    [logbug.catcher :as catcher]
    [logbug.debug :as debug]
    ))

(defn some-lending-manager? [request]
  (if (some
        #(:lending_manager (-> % :role roles/expand-to-hierarchy set))
        (->> request :authenticated-entity :access-rights))
    true
    false))

(defn some-lending-manager-and-http-safe? [request]
  (and (some-lending-manager? request)
       (http-safe? request)))

(defn pool-access-right-for-route [request]
  (let [inventory-pool-id (-> request :route-params :inventory-pool-id)]
    (->> request :authenticated-entity :access-rights
         (filter #(= (str (:inventory_pool_id %)) inventory-pool-id))
         first)))

(defn pool-lending-manager? [request]
  (if (if-let [access-right (pool-access-right-for-route request)]
        (#{"lending_manager" "inventory_manager"} (:role access-right))
        false)
    true
    false))

(defn pool-lending-manager-and-http-safe? [request]
  (if (if-let [access-right (pool-access-right-for-route request)]
        (#{"lending_manager" "inventory_manager"} (:role access-right))
        false)
    (if-not (http-safe? request)
      false
      true)))

(defn pool-inventory-manager? [request]
  (if (if-let [access-right (pool-access-right-for-route request)]
        (#{"inventory_manager"} (:role access-right))
        false)
    true
    false))


;#### debug ###################################################################

;(debug/wrap-with-log-debug #'activity-filter)
;(debug/wrap-with-log-debug #'set-order)
;(debug/wrap-with-log-debug #'inventory-pools-query)
;(debug/wrap-with-log-debug #'inventory-pools-formated-query)


;(-> *request* :route-params :inventory-pool-id)

;(pool-access-right-for-route *request*)
;(pool-lending-manager? *request*)

;(debug/debug-ns *ns*)
