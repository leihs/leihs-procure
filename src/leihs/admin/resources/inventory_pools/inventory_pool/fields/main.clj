(ns leihs.admin.resources.inventory-pools.inventory-pool.fields.main
  (:require
   [clojure.core.match :refer [match]]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc.sql :refer [query delete! insert!] :rename {query jdbc-query,
                                                          delete! jdbc-delete!
                                                          insert! jdbc-insert!}]))

(defn- select-fields [tx inventory-pool-id]
  (-> (sql/select :fields.id
                  :data
                  :dynamic
                  [[:case
                    [:is :disabled_fields.field_id :null] false
                    :else true]
                   :is_disabled])
      (sql/from :fields)
      (sql/left-join :disabled_fields [:and
                                       [:= :disabled_fields.inventory_pool_id inventory-pool-id]
                                       [:= :disabled_fields.field_id :fields.id]])
      (sql/where [:= :active true])
      (sql/order-by :id)
      sql-format
      (->> (jdbc-query tx))))

(defn get-fields
  [{{inventory-pool-id :inventory-pool-id} :route-params tx :tx}]
  {:body
   {:fields
    (select-fields tx inventory-pool-id)}})

(defn put-fields
  [{{pool-id :inventory-pool-id} :route-params
    tx :tx data :body}]
  (let [current-data (select-fields tx pool-id)]
    (doseq [{:keys [id is_disabled]} current-data]
      (some->> data (filter #(= (:id %) (str id))) first
               :is_disabled
               (#(match [is_disabled %]
                   [true false] (jdbc-delete! tx :disabled_fields {:field_id id :inventory_pool_id pool-id})
                   [false true] (jdbc-insert! tx :disabled_fields {:field_id id :inventory_pool_id pool-id})
                   :else nil))))
    {:status 204}))

(defn routes [request]
  (case (:request-method request)
    :get (get-fields request)
    :put (put-fields request)))
