(ns leihs.admin.resources.inventory-pools.inventory-pool.suspension.core
  (:refer-clojure :exclude [str keyword])
  (:require
   [clj-time.coerce]
   [clj-time.format]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [str]]
   [next.jdbc.sql :refer [insert! query update! delete!] :rename {query jdbc-query,
                                                                  insert! jdbc-insert!,
                                                                  update! jdbc-update!,
                                                                  delete! jdbc-delete!}])
  (:import
   [java.sql Date]))

(defn suspension-query [inventory-pool-id user-id]
  (-> (sql/select :*)
      (sql/from :suspensions)
      (sql/where [:= :inventory_pool_id inventory-pool-id])
      (sql/where [:= :user_id user-id])))

(defn suspension [tx inventory-pool-id user-id]
  (some-> (->> (-> (suspension-query inventory-pool-id user-id)
                   (sql/where [:raw  "CURRENT_DATE <= suspended_until"])
                   sql-format)
               (jdbc-query tx)
               first)
          (update-in [:suspended_until] str)))

(defn get-suspension
  [{{inventory-pool-id :inventory-pool-id :as route-params} :route-params
    tx :tx-next :as request}]
  (let [user-id (or (:user-id route-params) (:delegation-id route-params))]
    (if-let [suspension (suspension tx inventory-pool-id user-id)]
      {:status 200, :body suspension}
      {:status 200, :body {}})))

(defn set-suspension
  [{{inventory-pool-id :inventory-pool-id :as route-params} :route-params
    tx :tx-next body :body :as request}]
  (let [user-id (or (:user-id route-params) (:delegation-id route-params))]
    (jdbc-delete! tx :suspensions ["inventory_pool_id = ? AND user_id = ?" inventory-pool-id user-id])
    (let [data (-> body
                   (select-keys [:suspended_reason :suspended_until])
                   (update-in [:suspended_until] #(some-> % clj-time.format/parse
                                                          clj-time.coerce/to-long
                                                          Date.)))]
      (when (-> data :suspended_until)
        (jdbc-insert! tx :suspensions (assoc data :inventory_pool_id inventory-pool-id :user_id user-id))))
    (-> (get-suspension request) (assoc :status 200))))

;;; delete ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-suspension
  [{{inventory-pool-id :inventory-pool-id :as route-params} :route-params
    tx :tx-next body :body :as request}]
  (let [user-id (or (:user-id route-params) (:delegation-id route-params))]
    (jdbc-delete! tx :suspensions ["inventory_pool_id = ? AND user_id = ?" inventory-pool-id user-id])
    {:status 204}))
