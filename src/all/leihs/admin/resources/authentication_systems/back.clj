(ns leihs.admin.resources.authentication-systems.back
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]

    [leihs.admin.auth.back :as admin-auth]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.authentication-system.back :as authentication-system]
    [leihs.admin.resources.authentication-systems.shared :as shared]


    [leihs.core.sql :as sql]

    [clojure.java.jdbc :as jdbc]
    [clojure.set]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))


(def authentication-systems-base-query
  (-> (apply sql/select shared/default-fields)
      (sql/merge-select
        [(-> (sql/select :%count.*)
             (sql/from :authentication-systems_users)
             (sql/merge-where
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

(defn authentication-systems-formated-query [request]
  (-> request
      authentication-systems-query
      sql/format))

(defn authentication-systems [request]
  (when (= :json (-> request :accept :mime))
    {:body
     {:authentication-systems
      (jdbc/query (:tx request) (authentication-systems-formated-query request))}}))

(def routes
  (-> (cpj/routes
        (cpj/GET (path :authentication-systems) [] #'authentication-systems)
        (cpj/POST (path :authentication-systems ) [] #'authentication-system/routes))
      (admin-auth/wrap-authorize #{} {:scope_admin_read true
                                      :scope_admin_write true
                                      :scope_system_admin_read true
                                      :scope_system_admin_write true})))

;#### debug ###################################################################
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'authentication-systems-formated-query)
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
