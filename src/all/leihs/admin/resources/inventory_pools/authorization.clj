(ns leihs.admin.resources.inventory-pools.authorization
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]

    [leihs.admin.auth.authorization :refer [http-safe?]]

    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.back :as inventory-pool]
    [leihs.admin.resources.inventory-pools.shared :as shared :refer [inventory-pool-path]]

    [clojure.set :refer [rename-keys]]
    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [logbug.catcher :as catcher]
    ))

(defn http-safe-and-some-pools-lending-manger? [request]
  (if (and (http-safe? request)
           (->> request :authenticated-entity :access-rights
                (some #(or (#{"lending_manager" "inventory_manager"} (:role %))))
                boolean))
    {:allowed? true}
    {:allowed? false}))

(defn pool-access-right-for-route [request]
  (let [inventory-pool-id (-> request :route-params :inventory-pool-id)]
    (->> request :authenticated-entity :access-rights
         (filter #(= (str (:inventory_pool_id %)) inventory-pool-id))
         first)))

(defn pool-lending-manager? [request]
  (if (if-let [access-right (pool-access-right-for-route request)]
        (#{"lending_manager" "inventory_manager"} (:role access-right))
        false)
    {:allowed? true}
    {:allowed? false}))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(debug/wrap-with-log-debug #'activity-filter)
;(debug/wrap-with-log-debug #'set-order)
;(debug/wrap-with-log-debug #'inventory-pools-query)
;(debug/wrap-with-log-debug #'inventory-pools-formated-query)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)

;(-> *request* :route-params :inventory-pool-id)

;(pool-access-right-for-route *request*)
;(pool-lending-manager? *request*)
