(ns leihs.admin.resources.audits.requests.request.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
   [compojure.core :as cpj]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.paths :refer [path]]
   [leihs.core.uuid :refer [uuid]]
   [logbug.catcher :as catcher]
   [logbug.debug :as debug]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
   [taoensso.timbre :refer [error warn info debug spy]]))

(def selects
  [[:audited_requests.method :method]
   [:audited_requests.path :path]
   [:audited_responses.status :status]
   [:audited_requests.http_uid :http_uid]
   [:audited_requests.created_at :request_timestamp]
   [:audited_responses.created_at :response_timestamp]
   [:audited_requests.user_id :requester_id]])

(defn get-request
  [{{request-id :request-id} :route-params
    tx-next :tx-next :as request}]
  (assert request-id)
  {:body (-> (apply sql/select selects)
             (sql/from :audited_requests)
             (sql/where [:= :audited_requests.id (uuid request-id)])
             (sql/left-join
              :audited_responses
              [:= :audited_requests.txid :audited_responses.txid])
             sql-format
             (#(jdbc/execute-one! tx-next %)))})

(defn routes [request]
  (case (:handler-key request)
    :audited-request (get-request request)))

;#### debug ###################################################################

;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/wrap-with-log-debug #'users-formated-query)
