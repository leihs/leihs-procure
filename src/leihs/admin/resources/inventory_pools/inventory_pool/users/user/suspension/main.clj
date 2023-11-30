(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.suspension.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
   [compojure.core :as cpj]
   [leihs.admin.paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.suspension.core :as suspension]
   [logbug.debug :as debug]
   [taoensso.timbre :refer [error warn info debug spy]]))

;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def inventory-pool-user-suspension-path
  (path :inventory-pool-user-suspension {:inventory-pool-id ":inventory-pool-id" :user-id ":user-id"}))

(def routes
  (cpj/routes
   (cpj/DELETE inventory-pool-user-suspension-path [] #'suspension/delete-suspension)
   (cpj/GET inventory-pool-user-suspension-path [] #'suspension/get-suspension)
   (cpj/PUT inventory-pool-user-suspension-path [] #'suspension/set-suspension)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'filter-by-access-right)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/debug-ns *ns*)
