(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.suspension.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.users.main :as users]
    [leihs.admin.resources.inventory-pools.inventory-pool.suspension.core :as suspension]
    [leihs.admin.utils.regex :as regex]
    [leihs.core.sql :as sql]
    [leihs.admin.utils.jdbc :as utils.jdbc]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [clojure.set :as set]

    [clj-time.format]
    [clj-time.coerce]


    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    )
  (:import [java.sql Date]))

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
