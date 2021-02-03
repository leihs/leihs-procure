(ns leihs.admin.resources.inventory-pools.inventory-pool.suspension.core
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
  [{{inventory-pool-id :inventory-pool-id :as route-params} :route-params
    tx :tx :as request}]
  (let [user-id (or (:user-id route-params) (:delegation-id route-params))]
    (if-let [suspension (suspension tx inventory-pool-id user-id)]
      {:body suspension}
      {:body {}})))

(defn set-suspension
  [{{inventory-pool-id :inventory-pool-id :as route-params} :route-params
    tx :tx body :body :as request}]
  (let [user-id (or (:user-id route-params) (:delegation-id route-params))]
    (jdbc/delete! tx :suspensions ["inventory_pool_id = ? AND user_id = ?" inventory-pool-id user-id])
    (let [data (-> body
                   (select-keys [:suspended_reason :suspended_until])
                   (update-in [:suspended_until] #(some-> % clj-time.format/parse
                                                          clj-time.coerce/to-long
                                                          Date.)))]
      (when (-> data :suspended_until)
        (jdbc/insert! tx :suspensions (assoc data :inventory_pool_id inventory-pool-id :user_id user-id))))
    (get-suspension request)))


;;; delete ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-suspension
  [{{inventory-pool-id :inventory-pool-id :as route-params} :route-params
    tx :tx body :body :as request}]
  (let [user-id (or (:user-id route-params) (:delegation-id route-params))] 
    (jdbc/delete! tx :suspensions ["inventory_pool_id = ? AND user_id = ?" inventory-pool-id user-id])
    {:status 204}))
