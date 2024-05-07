(ns leihs.admin.resources.inventory-pools.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.resources.inventory-pools.inventory-pool.main :as inventory-pool]
   [leihs.admin.resources.inventory-pools.shared :as shared]
   [leihs.admin.utils.seq :as seq]
   [leihs.core.core :refer [keyword presence str]]
   [leihs.core.routing.back :as routing :refer [mixin-default-query-params
                                                set-per-page-and-offset]]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
   [taoensso.timbre :refer [spy]]))

(def users-count-sub
  (-> (sql/select :%count.*)
      (sql/from :users)
      (sql/join :access_rights [:= :access_rights.user_id :users.id])
      (sql/where [:= nil :delegator_user_id])
      (sql/where [:= :access_rights.inventory_pool_id :inventory_pools.id])))

(def delegations-count-sub
  (-> (sql/select :%count.*)
      (sql/from [:users :delegations])
      (sql/join :access_rights [:= :access_rights.user_id :delegations.id])
      (sql/where [:<> nil :delegator_user_id])
      (sql/where [:= :access_rights.inventory_pool_id :inventory_pools.id])))

(def inventory-pools-base-query
  (-> (apply sql/select (map #(keyword (str "inventory-pools." %)) shared/default-fields))
      (sql/select [users-count-sub :users_count])
      (sql/select [delegations-count-sub :delegations_count])
      (sql/from :inventory_pools)))

(defn set-order [query {query-params :query-params :as request}]
  (let [order (some-> query-params :order seq vec
                      (->> (map (fn [[f o]] [(keyword f) (keyword o)]))))]
    (case order
      ([[:name :asc] [:id :asc]]
       [[:users_count :desc] [:id :asc]]
       [[:delegations_count :desc] [:id :asc]]) (apply sql/order-by query order)
      (apply sql/order-by query [[:name :asc] [:id :asc]]))))

(defn term-filter [query request]
  (if-let [term (-> request :query-params-raw :term presence)]
    (-> query
        (sql/where [:or
                    [:% (str term) :name]
                    [(keyword "~~*") :name (str "%" term "%")]]))
    query))

(defn activity-filter [query request]
  (case (-> request :query-params-raw :active)
    "yes" (sql/where query [:= :inventory_pools.is_active true])
    "no" (sql/where query [:= :inventory_pools.is_active false])
    query))

(defn with-items-from-suppliers-filter [query request]
  (case (-> request :query-params-raw :with_items_from_suppliers)
    "yes" (-> query
              (dissoc :select)
              (sql/select-distinct :inventory_pools.id :inventory_pools.name)
              (sql/join :items [:= :items.inventory_pool_id :inventory_pools.id])
              (sql/join :suppliers [:= :items.supplier_id :suppliers.id]))
    query))

(defn inventory-pools-query [{:as request}]
  (-> inventory-pools-base-query
      (set-per-page-and-offset request)
      (set-order request)
      (activity-filter request)
      (term-filter request)
      (with-items-from-suppliers-filter request)))

(defn inventory-pools [{tx :tx :as request}]
  (let [query (inventory-pools-query request)
        offset (:offset query)]
    {:body {:inventory-pools
            (-> query
                sql-format
                (->> (jdbc-query tx)
                     (seq/with-index offset)))}}))

(def routes
  (fn [request]
    (case (:request-method request)
      :get (-> request
               (mixin-default-query-params shared/default-query-params)
               inventory-pools)
      :post (inventory-pool/create-inventory-pool request))))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'activity-filter)
;(debug/wrap-with-log-debug #'set-order)
;(debug/wrap-with-log-debug #'inventory-pools-query)
;(debug/wrap-with-log-debug #'inventory-pools-formated-query)

;(debug/debug-ns *ns*)
