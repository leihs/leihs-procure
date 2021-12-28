(ns leihs.admin.resources.audits.requests.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.string :as string]
    [clojure.tools.logging :as logging]
    [compojure.core :as cpj]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.audits.requests.shared :refer [default-query-params]]
    [leihs.admin.resources.users.choose-core :as choose-user]
    [leihs.core.auth.core :as auth]
    [leihs.core.routing.back :as routing :refer [set-per-page-and-offset wrap-mixin-default-query-params]]
    [leihs.core.sql :as sql]
    [logbug.catcher :as catcher]
    [logbug.debug :as debug]
    ))

(def auditec-requests-select
  [[:audited_requests.txid :txid]
   [:audited_requests.http_uid :http_uid]
   [:audited_requests.created_at :request_timestamp]
   [:audited_requests.method :method]
   [:audited_requests.path :path]
   [:users.id :requester_id]
   [:users.email :requester_email]
   [:users.login :requester_login]
   [:audited_responses.status :response_status]
   ])

(def requests-base-query
  (-> (apply sql/select auditec-requests-select)
      (sql/from :audited_requests)
      (sql/order-by [:audited_requests.created_at :desc])
      (sql/merge-left-join :users
                           [:= :audited_requests.user_id :users.id])
      (sql/merge-left-join :audited_responses
                           [:= :audited_requests.txid :audited_responses.txid])))

(defn filter-by-user-uid [query {{user-uid :user-uid} :query-params}]
  (if-let [user-uid (presence user-uid)]
    (sql/merge-where
      query
      [:exists
       (-> (choose-user/find-by-some-uid-query user-uid)
           (sql/select :true)
           (sql/merge-where [:= :users.id :audited_requests.user_id]))])
    query))

(defn filter-by-method [query {{method :method} :query-params}]
  (if-let [method (some-> method presence string/lower-case)]
    (sql/merge-where query [:= :audited_requests.method method])
    query))


(defn audited-requests [{tx :tx :as request}]
  {:body {:requests
          (->> (-> requests-base-query
                   (filter-by-user-uid request)
                   (filter-by-method request)
                   (set-per-page-and-offset request)
                   sql/format)
               (jdbc/query tx))}})

(def routes
  (-> (cpj/routes
        ;(cpj/GET (path :audited-changes-meta {}) [] #'audited-changes-meta)
        (cpj/GET (path :audited-requests {}) [] #'audited-requests))
      (wrap-mixin-default-query-params default-query-params)))

;#### debug ###################################################################
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/wrap-with-log-debug #'users-formated-query)
