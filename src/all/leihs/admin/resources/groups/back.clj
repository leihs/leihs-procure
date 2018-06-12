(ns leihs.admin.resources.groups.back
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.admin.utils.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.group.back :as group]
    [leihs.admin.resources.groups.shared :as shared]
    [leihs.admin.utils.sql :as sql]

    [clojure.java.jdbc :as jdbc]
    [clojure.set]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))


(def groups-base-query
  (-> (apply sql/select shared/default-fields)
      (sql/merge-select 
        [(-> (sql/select :%count.*)
             (sql/from :groups_users)
             (sql/merge-where 
               [:= :groups_users.group_id :groups.id]))
         :count_users])
      (sql/from :groups)
      (sql/order-by :name :id)))

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

(defn term-fitler [query request]
  (if-let [term (-> request :query-params :term presence)]
    (-> query
        (sql/merge-where [:or
                          ["%" (str term) :searchable]
                          ["~~*" :searchable (str "%" term "%")]]))
    query))

(defn type-filter [query request]
  (case (-> request :query-params :type)
    (nil "any") query
    "org" (-> query
              (sql/merge-where [:<> nil :org_id]))
    "manual" (-> query
                 (sql/merge-where [:= nil :org_id]))))


(defn select-fields [query request]
  (if-let [fields (some->> request :query-params :fields 
                           (map keyword) set 
                           (clojure.set/intersection shared/available-fields))]
    (apply sql/select query fields)
    query))

(defn groups-query [request]
  (let [query-params (-> request :query-params 
                         shared/normalized-query-parameters)]
    (-> groups-base-query
        (set-per-page-and-offset query-params)
        (term-fitler request)
        (type-filter request)
        (select-fields request))))

(defn groups-formated-query [request]
  (-> request
      groups-query
      sql/format))

(defn groups [request]
  (when (= :json (-> request :accept :mime))
    {:body
     {:groups
      (jdbc/query (:tx request) (groups-formated-query request))}}))

(def routes
  (cpj/routes
    (cpj/GET (path :groups) [] #'groups)
    (cpj/POST (path :groups ) [] #'group/routes)))

;#### debug ###################################################################
;(logging-config/set-logger! :level :info)
;(debug/debug-ns 'cider-ci.utils.shutdown)
;(debug/debug-ns *ns*)

;(logging-config/set-logger! :level :debug)
;(debug/wrap-with-log-debug #'groups-formated-query)
