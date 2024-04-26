(ns leihs.admin.resources.suppliers.supplier.items
  (:refer-clojure :exclude [str keyword])
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.uuid :refer [uuid]]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

;;; items ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn supplier-items-query [supplier-id]
  (-> (sql/select :items.id
                  :items.inventory_code
                  :models.product
                  :models.version
                  [:models.id :model_id]
                  [:inventory_pools.name :inventory_pool_name]
                  :inventory_pool_id)
      (sql/from :items)
      (sql/join :models [:= :items.model_id :models.id])
      (sql/join :inventory_pools [:= :items.inventory_pool_id :inventory_pools.id])
      (sql/where [:= :supplier_id (uuid supplier-id)])
      (sql/where [:= :inventory_pools.is_active true])
      (sql/order-by [:inventory_pool_name :asc] [:items.inventory_code :asc])
      sql-format))

(defn supplier-items [tx-next supplier-id]
  (->> supplier-id
       supplier-items-query
       (jdbc-query tx-next)))

(defn get-supplier-items
  [{tx-next :tx-next {supplier-id :supplier-id} :route-params}]
  {:body {:items (supplier-items tx-next supplier-id)}})

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [request]
  (case (:request-method request)
    :get (get-supplier-items request)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)

;(debug/debug-ns *ns*)
