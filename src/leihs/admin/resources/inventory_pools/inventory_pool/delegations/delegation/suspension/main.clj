(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.suspension.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.suspension.core :as suspension]
    [leihs.admin.utils.jdbc :as utils.jdbc]
    [leihs.core.sql :as sql]
    [logbug.debug :as debug]))

;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def inventory-pool-delegation-suspension-path
  (path :inventory-pool-delegation-suspension
        {:inventory-pool-id ":inventory-pool-id" :delegation-id ":delegation-id"}))

(def routes
  (cpj/routes
    (cpj/DELETE inventory-pool-delegation-suspension-path [] #'suspension/delete-suspension)
    (cpj/GET inventory-pool-delegation-suspension-path [] #'suspension/get-suspension)
    (cpj/PUT inventory-pool-delegation-suspension-path [] #'suspension/set-suspension)))


;#### debug ###################################################################


;(debug/wrap-with-log-debug #'filter-by-access-right)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/debug-ns *ns*)
