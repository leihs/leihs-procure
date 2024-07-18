(ns leihs.admin.resources.inventory-fields.inventory-field.inventory-pools.main
  (:require
   [clojure.core.match :refer [match]]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc.sql :refer [query delete! insert!] :rename {query jdbc-query,
                                                          delete! jdbc-delete!
                                                          insert! jdbc-insert!}]))

(defn- select-inventory-pools [tx field-id]
  (-> (sql/select :inventory_pools.id
                  :name
                  :shortname
                  [[:case
                    [:is :disabled_fields.field_id :null] false
                    :else true]
                   :is_disabled])
      (sql/from :inventory_pools)
      (sql/left-join :disabled_fields [:and
                                       [:= :disabled_fields.inventory_pool_id :inventory_pools.id]
                                       [:= :disabled_fields.field_id field-id]])
      (sql/where [:= :is_active true])
      (sql/order-by :name :id)
      sql-format
      (->> (jdbc-query tx))))

(defn get-inventory-pools
  [{{inventory-field-id :inventory-field-id} :route-params tx :tx}]
  {:body
   {:inventory-pools
    (select-inventory-pools tx inventory-field-id)}})

(defn put-inventory-pools
  [{{field-id :inventory-field-id} :route-params
    tx :tx data :body}]
  (let [current-data (select-inventory-pools tx field-id)]
    (doseq [{:keys [id is_disabled]} current-data]
      (some->> data (filter #(= (:id %) (str id))) first
               :is_disabled
               (#(match [is_disabled %]
                   [true false] (jdbc-delete! tx :disabled_fields {:field_id field-id :inventory_pool_id id})
                   [false true] (jdbc-insert! tx :disabled_fields {:field_id field-id :inventory_pool_id id})
                   :else nil))))
    {:status 204}))

(defn routes [request]
  (case (:request-method request)
    :get (get-inventory-pools request)
    :put (put-inventory-pools request)))
