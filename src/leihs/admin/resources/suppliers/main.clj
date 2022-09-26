(ns leihs.admin.resources.suppliers.main
  (:refer-clojure :exclude [str keyword])
  (:require
    [clojure.set]
    [compojure.core :as cpj]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.suppliers.shared :as shared]
    [leihs.admin.resources.suppliers.supplier.main :as supplier]
    [leihs.admin.utils.seq :as seq]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.uuid :refer [uuid]]
    [logbug.debug :as debug]
    [next.jdbc.sql :as jdbc]
    [taoensso.timbre :refer [error warn info debug spy]]
    ))

(def suppliers-base-query
  (-> (sql/select-distinct :suppliers.id
                           :suppliers.name
                           :suppliers.note
                           [(-> (sql/select :%count.*)
                                (sql/from :items)
                                (sql/where := :items.supplier_id :suppliers.id))
                            :count_items])
      (sql/from :suppliers)
      (sql/order-by :suppliers.name :suppliers.id)))

(defn set-per-page-and-offset
  ([query {per-page :per-page page :page}]
   (when (or (-> per-page presence not)
             (-> per-page integer? not)
             (> per-page 1000)
             (< per-page 1))
     (throw (ex-info "The query parameter per-page must be present and set to an integer between 1 and 1000."
                     {:status 422})))
   (when (or (-> page presence not)
             (-> page integer? not)
             (< page 0))
     (throw (ex-info "The query parameter page must be present and set to a positive integer."
                     {:status 422})))
   (set-per-page-and-offset query per-page page))
  ([query per-page page]
   (-> query
       (sql/limit per-page)
       (sql/offset (* per-page (- page 1))))))

(def searchable-expr [:concat :name " " :note])

(defn term-filter [query request]
  (if-let [term (-> request :query-params-raw :term presence)]
    (-> query
        (sql/where [:ilike searchable-expr (str "%" term "%")]))
    query))

(defn inventory-pool-filter [query request]
  (if-let [pool-id (-> request :query-params-raw :inventory_pool_id presence)]
    (-> query
        (sql/join :items [:= :items.supplier_id :suppliers.id])
        (sql/join :inventory_pools [:= :items.inventory_pool_id :inventory_pools.id])
        (sql/where [:= :inventory_pools.id (uuid pool-id)]))
    query))

(defn suppliers-query [request]
  (let [query-params  (-> request :query-params
                          shared/normalized-query-parameters)]
    (-> suppliers-base-query
        (set-per-page-and-offset query-params)
        (term-filter request)
        (inventory-pool-filter request))))

(defn suppliers [{tx-next :tx-next :as request}]
  (let [query (suppliers-query request)
        offset (:offset query)]
    {:body
     {:suppliers (-> query
                     sql-format
                     (->> (jdbc/query tx-next)
                          (seq/with-index offset)
                          seq/with-page-index))}}))

;;; create supplier ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-supplier [{tx-next :tx-next data :body :as request}]
  (if-let [supplier (jdbc/insert! tx-next
                                  :suppliers
                                  (select-keys data supplier/fields))]
    {:body supplier}
    {:status 422
     :body "No supplier has been created."}))

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def suppliers-path (path :suppliers))

(def routes
  (cpj/routes
    (cpj/GET suppliers-path [] #'suppliers)
    (cpj/POST suppliers-path [] #'create-supplier)))

;#### debug ###################################################################
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'groups-formated-query)
