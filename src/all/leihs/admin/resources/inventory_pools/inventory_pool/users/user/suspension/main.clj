(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.suspension.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.users.main :as users]
    [leihs.admin.utils.regex :as regex]
    [leihs.core.sql :as sql]
    [leihs.admin.utils.jdbc :as utils.jdbc]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [clojure.set :as set]

    [clj-time.format]
    [clj-time.coerce]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    )
  (:import [java.sql Date]))

;;; suspension ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn suspension-query [inventory-pool-id user-id]
  (-> (sql/select :*)
      (sql/from :suspensions)
      (sql/merge-where [:= :inventory_pool_id inventory-pool-id])
      (sql/merge-where [:= :user_id user-id])))

(defn suspension [tx inventory-pool-id user-id]
  (some-> (->> (-> (suspension-query inventory-pool-id user-id)
                   (sql/merge-where (sql/raw  "CURRENT_DATE <= suspended_until"))
                   sql/format)
               (jdbc/query tx)
               first )
          (update-in [:suspended_until] str)))

(defn get-suspension
  [{{inventory-pool-id :inventory-pool-id user-id :user-id} :route-params
    tx :tx :as request}]
  (if-let [suspension (suspension tx inventory-pool-id user-id)]
    {:body suspension}
    {:body {}}))

(defn text-date->sql-date [td])

(defn set-suspension
  [{{inventory-pool-id :inventory-pool-id user-id :user-id} :route-params
    tx :tx body :body :as request}]
  (jdbc/delete! tx :suspensions ["inventory_pool_id = ? AND user_id = ?" inventory-pool-id user-id])
  (let [data (-> body
                 (select-keys [:suspended_reason :suspended_until])
                 (update-in [:suspended_until] #(some-> % clj-time.format/parse
                                                        clj-time.coerce/to-long
                                                        Date.)))]
    (when (-> data :suspended_until)
      (jdbc/insert! tx :suspensions (assoc data :inventory_pool_id inventory-pool-id :user_id user-id))))
  {:status 204})


;;; delete ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-suspension
  [{{inventory-pool-id :inventory-pool-id user-id :user-id} :route-params
    tx :tx body :body :as request}]
  (jdbc/delete! tx :suspensions ["inventory_pool_id = ? AND user_id = ?" inventory-pool-id user-id])
  {:status 204})


;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def inventory-pool-user-suspension-path
  (path :inventory-pool-user-suspension {:inventory-pool-id ":inventory-pool-id" :user-id ":user-id"}))

(def routes
  (cpj/routes
    (cpj/DELETE inventory-pool-user-suspension-path [] #'delete-suspension)
    (cpj/GET inventory-pool-user-suspension-path [] #'get-suspension)
    (cpj/PUT inventory-pool-user-suspension-path [] #'set-suspension)))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/wrap-with-log-debug #'filter-by-access-right)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/debug-ns *ns*)
