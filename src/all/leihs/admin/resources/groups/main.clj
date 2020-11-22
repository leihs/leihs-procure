(ns leihs.admin.resources.groups.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.groups.group.main :as group]
    [leihs.admin.resources.groups.shared :as shared]
    [leihs.admin.utils.regex :refer [uuid-pattern]]
    [leihs.core.sql :as sql]

    [clojure.java.jdbc :as jdbc]
    [clojure.set]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))


(def groups-base-query
  (-> (apply sql/select (map #(keyword (str "groups." %)) shared/default-fields))
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

(defn term-filter [query request]
  (if-let [term (-> request :query-params-raw :term presence)]
    (-> query
        (sql/merge-where [:or
                          ["%" (str term) :groups.searchable]
                          ["~~*" :groups.searchable (str "%" term "%")]]))
    query))

(defn org-filter [query request]
  (let [qp (presence (or (some-> request :query-params-raw :org_id)
               (some-> request :query-params-raw :type)))]
    (case qp
      (nil "any") query
      ("true" "org") (sql/merge-where query [:<> nil :org_id])
      ("false" "manual") (sql/merge-where query [:= nil :org_id])
      (sql/merge-where query [:= :org_id (str qp)]))))

(defn filter-for-including-user-by-email [query email-term]
  (-> query
      (sql/merge-join :groups_users [:= :groups_users.group_id :groups.id])
      (sql/merge-join :users [:= :groups_users.user_id :users.id])
      (sql/merge-where [:= (sql/call :lower email-term) (sql/call :lower :users.email)])))

(defn filter-for-including-user-by-id [query id-term]
  (-> query
      (sql/merge-join :groups_users [:= :groups_users.group_id :groups.id])
      (sql/merge-join :users [:= :groups_users.user_id :users.id])
      (sql/merge-where [:= (sql/call :cast id-term :uuid) :users.id])))

(defn filter-for-including-user
  [query {{user-term :including-user} :query-params :as request}]
  (cond
    (nil? (presence user-term)) query
    (clojure.string/includes? user-term "@") (filter-for-including-user-by-email
                                               query user-term)
    (re-matches uuid-pattern user-term) (filter-for-including-user-by-id
                                          query user-term)
    :else (sql/merge-where query false)))

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
        (term-filter request)
        (org-filter request)
        (filter-for-including-user request)
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
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'groups-formated-query)
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
