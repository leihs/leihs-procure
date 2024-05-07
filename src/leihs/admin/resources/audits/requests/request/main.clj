(ns leihs.admin.resources.audits.requests.request.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.uuid :refer [uuid]]
   [next.jdbc :as jdbc]))

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
    tx :tx :as request}]
  (assert request-id)
  {:body (-> (apply sql/select selects)
             (sql/from :audited_requests)
             (sql/where [:= :audited_requests.id (uuid request-id)])
             (sql/left-join
              :audited_responses
              [:= :audited_requests.txid :audited_responses.txid])
             sql-format
             (#(jdbc/execute-one! tx %)))})

(defn routes [request]
  (case (:handler-key request)
    :audited-request (get-request request)))

;#### debug ###################################################################

;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/wrap-with-log-debug #'users-formated-query)
