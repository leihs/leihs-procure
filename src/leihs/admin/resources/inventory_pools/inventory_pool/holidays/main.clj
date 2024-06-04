(ns leihs.admin.resources.inventory-pools.inventory-pool.holidays.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.db :as db]
   [leihs.core.uuid :refer [uuid]]
   [next.jdbc.sql :refer [delete! insert! query update!]
    :rename {delete! jdbc-delete! insert! jdbc-insert!
             query jdbc-query, update! jdbc-update!}]
   [next.jdbc.types :refer [as-date]]
   [taoensso.timbre :refer [error warn info debug spy]]))

(def fields #{:id
              :name
              :start_date
              :end_date
              :inventory_pool_id})

(defn get-holidays
  [{{inventory-pool-id :inventory-pool-id} :route-params tx :tx :as request}]
  {:body (-> (apply sql/select fields)
             (sql/from :holidays)
             (sql/where [:= :inventory_pool_id inventory-pool-id])
             (sql/where [:>= :end_date :current_date])
             (sql/order-by :end_date)
             sql-format
             (->> (jdbc-query tx)
                  (map #(-> %
                            (update :start_date str)
                            (update :end_date str)))))})

(defn filter-holidays
  [inventory-pool-id data]
  (filter #(and (= (:inventory_pool_id %) inventory-pool-id)
                (or (and (:id %) (:delete %))
                    (and (not (:id %))
                         (not (:delete %)))))
          data))

(defn patch-holidays
  "Only the following holiday data is accepted and processed by
  the patch handler:
  - where inventory_pool_id matches the route param
  - existing holidays which are to be deleted
    (new ones to be deleted don't make sense and are ignored)
  - new holidays
  UI doesn't offer possibility to update existing holidays."
  [{{inventory-pool-id :inventory-pool-id} :route-params
    tx :tx data :body}]
  (let [data2 (->> data
                   (map #(update % :inventory_pool_id uuid))
                   (filter-holidays inventory-pool-id))]
    (doseq [holiday data2]
      (if (:delete holiday)
        (jdbc-delete! tx :holidays {:id (uuid (:id holiday))})
        (jdbc-insert! tx :holidays (-> holiday
                                       (update :start_date as-date)
                                       (update :end_date as-date))))))
  {:status 204})

(defn routes [request]
  (case (:request-method request)
    :get (get-holidays request)
    :patch (patch-holidays request)))
