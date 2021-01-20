(ns leihs.admin.resources.groups.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.core.sql :as sql]

    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.groups.group.main :as group]
    [leihs.admin.resources.groups.shared :as shared]
    [leihs.admin.utils.regex :refer [uuid-pattern]]
    [leihs.admin.resources.users.choose-core :as choose-user]
    [leihs.admin.utils.seq :as seq]

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

(defn filter-for-including-user
  [query {{user-uid :including-user} :query-params-raw :as request}]
  (if-let [user-uid (presence user-uid)]
    (sql/merge-where
      query
      [:exists
       (-> (choose-user/find-by-some-uid-query user-uid)
           (sql/select :true)
           (sql/merge-join :groups_users [:= :groups_users.group_id :groups.id])
           (sql/merge-where [:= :groups_users.user_id :users.id]))])
    query))

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

(defn groups [{tx :tx :as request}]
  (let [query (groups-query request)
        offset (:offset query)]
    {:body
     {:groups (-> query
                  sql/format
                  (->> (jdbc/query tx)
                       (seq/with-index offset)
                       seq/with-page-index))}}))

(def routes
  (cpj/routes
    (cpj/GET (path :groups) [] #'groups)
    (cpj/POST (path :groups ) [] #'group/routes)))

;#### debug ###################################################################
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'groups-formated-query)
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
