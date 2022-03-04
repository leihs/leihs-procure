(ns leihs.admin.resources.audits.requests.request.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.tools.logging :as logging]
    [compojure.core :as cpj]
    [leihs.admin.paths :refer [path]]
    [leihs.core.sql :as sql]
    [logbug.catcher :as catcher]
    [logbug.debug :as debug]
    ))

(def selects
  [[:audited_requests.method :method]
   [:audited_requests.path :path]
   [:audited_responses.status :status]
   [:audited_requests.http_uid :http_uid]
   [:audited_requests.created_at :request_timestamp]
   [:audited_responses.created_at :response_timestamp]
   [:audited_requests.user_id :requester_id]])

(defn get-request
  [{{txid :txid} :route-params
    tx :tx :as request}]
  {:body (->> (-> (apply sql/select selects)
                  (sql/from :audited_requests)
                  (sql/merge-where [:= :audited_requests.txid txid])
                  (sql/merge-left-join
                    :audited_responses
                    [:= :audited_requests.txid :audited_responses.txid])
                  sql/format)
              (jdbc/query tx) first )})

(defn routes [request]
  (case (:handler-key request)
    :audited-request (get-request request)))

;#### debug ###################################################################

;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/wrap-with-log-debug #'users-formated-query)
