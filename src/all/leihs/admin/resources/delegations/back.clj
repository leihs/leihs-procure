(ns leihs.admin.resources.delegations.back
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.admin.utils.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.utils.sql :as sql]
    [leihs.admin.resources.delegation.back :as delegation]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]

    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))


(def delegations-base-query
  (-> (sql/select :delegations.id, :delegations.firstname 
                  [(-> (sql/select :%count.*)
                       (sql/from :delegations_users)
                       (sql/merge-where 
                         [:= :delegations_users.delegation_id :delegations.id]))
                   :count_users]
                  [(-> (sql/select :%count.*)
                       (sql/from :contracts)
                       (sql/merge-where [:= :contracts.user_id :delegations.id]))
                   :count_contracts])
      (sql/from [:users :delegations])
      (sql/order-by :delegations.firstname)
      (sql/merge-where [:<> nil :delegations.delegator_user_id])))

(defn set-per-page-and-offset
  ([query {{per-page :per-page page :page} :query-params}]
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


(defn delegations-query [request]
  (-> delegations-base-query
      (set-per-page-and-offset request)
      (term-fitler request)
      sql/format))

(defn delegations [request]
  (when (= :json (-> request :accept :mime))
    {:body
     {:delegations
      (jdbc/query (:tx request) (delegations-query request))}}))


(def routes
  (cpj/routes
    (cpj/GET (path :delegations) [] #'delegations)
    (cpj/POST (path :delegations) [] #'delegation/routes)
    ))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns 'cider-ci.utils.shutdown)
;(debug/debug-ns *ns*)
