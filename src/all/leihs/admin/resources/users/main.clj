(ns leihs.admin.resources.users.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.core.sql :as sql]
    [leihs.admin.resources.users.user.main :as user]
    [leihs.admin.resources.users.queries :as queries]
    [leihs.admin.resources.users.shared :as shared]

    [clojure.java.jdbc :as jdbc]
    [clojure.set]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))


(def users-base-query
  (-> (apply sql/select (map #(keyword (str "users." %)) shared/default-fields))
      (sql/from :users)
      (sql/order-by :lastname :firstname :id)
      (sql/merge-where [:= nil :delegator_user_id])))

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

(defn match-term-with-emails [query term]
  (sql/merge-where
    query
    [:or
     [:= (sql/call :lower term) (sql/call :lower :users.email)]
     [:= (sql/call :lower term) (sql/call :lower :users.secondary_email)]]))

(defn match-term-fuzzy [query term]
  (sql/merge-where query [:or
                          ["%" (str term) :searchable]
                          ["~~*" :searchable (str "%" term "%")]]))

(defn term-filter [query request]
  (if-let [term (-> request :query-params-raw :term presence)]
    (if (clojure.string/includes? term "@" )
      (match-term-with-emails query term)
      (match-term-fuzzy query term))
    query))

(defn org-filter [query request]
  (let [qp (presence (or (some-> request :query-params-raw :org_id)
               (some-> request :query-params-raw :type)))]
    (case qp
      (nil "any") query
      ("true" "org") (sql/merge-where query [:<> nil :org_id])
      ("false" "manual") (sql/merge-where query [:= nil :org_id])
      (sql/merge-where query [:= :org_id (str qp)]))))

(defn account-enabled-filter [query request]
  (let [qp  (some-> request :query-params-raw :account_enabled)]
    (case qp
      (nil "any" "") query
      (true "true" "yes") (sql/merge-where query [:= true :account_enabled])
      (false "false" "no") (sql/merge-where query [:= false :account_enabled]))))

(defn is-admin-filter [query request]
  (let [qp  (some-> request :query-params-raw :is_admin)]
    (case qp
      (nil "any" "") query
      (true "true" "yes") (sql/merge-where query [:= true :is_admin])
      (false "false" "no") (sql/merge-where query [:= false :is_admin]))))

(defn select-fields [query request]
  (if-let [fields (some->> request :query-params :fields
                           (map keyword) set
                           (clojure.set/intersection shared/available-fields))]
    (apply sql/select query fields)
    query))

(defn users-query [request]
  (let [query-params (-> request :query-params
                         shared/normalized-query-parameters)]
    (-> users-base-query
        (set-per-page-and-offset query-params)
        (term-filter request)
        (org-filter request)
        (account-enabled-filter request)
        (is-admin-filter request)
        (select-fields request))))


(defn select-contract-counts [query]
  (-> query
      (sql/merge-select [queries/open-contracts-sub :open_contracts_count])
      (sql/merge-select [queries/closed-contracts-sub :closed_contracts_count])))

(defn users-formated-query [request]
  (-> request
      users-query
      select-contract-counts
      (sql/merge-select [queries/pools-count :pools_count])
      (sql/merge-select [queries/groups-count :groups_count])
      sql/format))

(defn users [request]
  (when (= :json (-> request :accept :mime))
    {:body
     {:users
      (jdbc/query (:tx request) (users-formated-query request))}}))

(def routes
  (cpj/routes
    (cpj/GET (path :users) [] #'users)
    (cpj/GET (path :users-choose) [] #'users)
    (cpj/POST (path :users) [] #'user/routes)))

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(debug/debug-ns *ns*)
;(logging-config/set-logger! :level :debug)
;(debug/wrap-with-log-debug #'org-filter)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/wrap-with-log-debug #'users-formated-query)
