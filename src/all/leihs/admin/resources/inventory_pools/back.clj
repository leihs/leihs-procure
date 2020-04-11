(ns leihs.admin.resources.inventory-pools.back
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.sql :as sql]

    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.back :as inventory-pool]
    [leihs.admin.resources.inventory-pools.shared :as shared :refer [inventory-pool-path]]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [logbug.catcher :as catcher]
    ))

(def inventory-pools-base-query
  (-> (apply sql/select (map #(keyword (str "inventory-pools." %)) shared/default-fields))
      (sql/merge-select
        [(-> (sql/select :%count.*)
             (sql/from :access_rights)
             (sql/merge-where
               [:= :access_rights.inventory_pool_id :inventory_pools.id]))
         :users_count])
      (sql/from :inventory_pools)
      (#(apply sql/order-by (concat [%] (-> shared/default-query-parameters :order))))))

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

(defn set-order [query query-params]
  (let [order (some-> query-params :order seq vec)]
    (case order
      nil query
      [["name" "asc"]
       ["id" "asc"]] (sql/order-by query
                                   [:name :asc] [:id :asc])
      [["users_count" "desc"]
       ["id" "asc"]] (sql/order-by
                       query [:users_count :desc] [:id :asc]))))

(defn term-fitler [query request]
  (if-let [term (-> request :query-params-raw :term presence)]
    (-> query
        (sql/merge-where [:or
                          ["%" (str term) :name]
                          ["~~*" :name (str "%" term "%")]]))
    query))


(defn activity-filter [query request]
  (case (-> request :query-params :is-active presence (or "all"))
    "all" query
    "active" (sql/merge-where query [:= :inventory_pools.is_active true])
    "inactive" (sql/merge-where query [:= :inventory_pools.is_active false])))


(defn select-fields [query request]
  (if-let [fields (some->> request :query-params :fields
                           (map keyword) set
                           (clojure.set/intersection shared/available-fields))]
    (apply sql/select query fields)
    query))

(defn inventory-pools-query [request]
  (let [query-params (-> request :query-params
                         shared/normalized-query-parameters)]
    (-> inventory-pools-base-query
        (set-per-page-and-offset query-params)
        (set-order query-params)
        (activity-filter request)
        (term-fitler request)
        (select-fields request))))

(defn inventory-pools-formated-query [request]
  (-> request
      inventory-pools-query
      sql/format))

(defn inventory-pools [request]
  (when (= :json (-> request :accept :mime))
    {:body
     {:inventory-pools
      (jdbc/query (:tx request) (inventory-pools-formated-query request))}}))

(def routes
  (->
    (cpj/routes
      (cpj/GET (path :inventory-pools) [] #'inventory-pools)
      (cpj/POST (path :inventory-pools) [] inventory-pool/routes)
      (cpj/ANY inventory-pool-path [] inventory-pool/routes))))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(debug/wrap-with-log-debug #'activity-filter)
;(debug/wrap-with-log-debug #'set-order)
;(debug/wrap-with-log-debug #'inventory-pools-query)
;(debug/wrap-with-log-debug #'inventory-pools-formated-query)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
