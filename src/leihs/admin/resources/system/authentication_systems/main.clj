(ns leihs.admin.resources.system.authentication-systems.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.resources.system.authentication-systems.authentication-system.main :as authentication-system]
   [leihs.admin.resources.system.authentication-systems.shared :as shared]
   [leihs.admin.utils.seq :as seq]
   [leihs.core.core :refer [keyword presence]]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query,
                                          insert! jdbc-insert!,
                                          update! jdbc-update!,
                                          delete! jdbc-delete!}]))

(def authentication-systems-base-query
  (-> (apply sql/select shared/default-fields)
      (sql/select
       [(-> (sql/select :%count.*)
            (sql/from :authentication-systems_users)
            (sql/where
             [:= :authentication-systems_users.authentication-system_id :authentication-systems.id]))
        :count_users])
      (sql/from :authentication-systems)
      (sql/order-by [:priority :desc] :name :id)))

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

(defn select-fields [query request]
  (if-let [fields (some->> request :query-params :fields
                           (map keyword) set
                           (clojure.set/intersection shared/available-fields))]
    (apply sql/select query fields)
    query))

(defn authentication-systems-query [request]
  (let [query-params (-> request :query-params
                         shared/normalized-query-parameters)]
    (-> authentication-systems-base-query
        (set-per-page-and-offset query-params)
        (select-fields request))))

(defn authentication-systems [{tx :tx :as request}]
  (let [query (authentication-systems-query request)
        offset (:offset query)]
    {:body
     {:authentication-systems
      (-> query
          sql-format
          (->> (jdbc-query tx)
               (seq/with-key :id)
               (seq/with-index offset)
               seq/with-page-index))}}))

(defn routes [request]
  (case (:request-method request)
    :get (authentication-systems request)
    :post (authentication-system/create-authentication-system request)))

;#### debug ###################################################################
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'authentication-systems-formated-query)
