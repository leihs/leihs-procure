(ns leihs.admin.resources.inventory-pools.inventory-pool.workdays.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc.sql :refer [query update!] :rename {query jdbc-query,
                                                  update! jdbc-update!}]
   [taoensso.timbre :refer [error warn info debug spy]]))

(def fields #{:id
              :inventory_pool_id
              :monday
              :tuesday
              :wednesday
              :thursday
              :friday
              :saturday
              :sunday
              :max_visits})

(defn get-workdays
  [{{inventory-pool-id :inventory-pool-id} :route-params tx :tx :as request}]
  {:body (-> (apply sql/select fields)
             (sql/from :workdays)
             (sql/where [:= :inventory_pool_id inventory-pool-id])
             sql-format
             (->> (jdbc-query tx))
             first)})

(defn patch-workdays
  [{{inventory-pool-id :inventory-pool-id} :route-params
    tx :tx data :body}]
  (jdbc-update! tx :workdays
                (select-keys data fields)
                ["inventory_pool_id = ?" inventory-pool-id])
  {:status 204})

(defn routes [request]
  (case (:request-method request)
    :get (get-workdays request)
    :patch (patch-workdays request)))
