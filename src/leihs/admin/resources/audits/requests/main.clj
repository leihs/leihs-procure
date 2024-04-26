(ns leihs.admin.resources.audits.requests.main
  (:require
   [clojure.string :as string]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.resources.audits.requests.shared :refer [default-query-params]]
   [leihs.admin.resources.users.user.core :refer [sql-where-unique-user]]
   [leihs.core.core :refer [presence]]
   [leihs.core.routing.back :as routing :refer [mixin-default-query-params
                                                set-per-page-and-offset]]
   [next.jdbc :as jdbc]))

(def auditec-requests-select
  [[:audited_requests.id :id]
   [:audited_requests.txid :txid]
   [:audited_requests.tx2id :tx2id]
   [:audited_requests.http_uid :http_uid]
   [:audited_requests.created_at :request_timestamp]
   [:audited_requests.method :method]
   [:audited_requests.path :path]
   [:users.id :requester_id]
   [:users.email :requester_email]
   [:users.login :requester_login]
   [:audited_responses.status :response_status]])

(def requests-base-query
  (-> (apply sql/select auditec-requests-select)
      (sql/from :audited_requests)
      (sql/order-by [:audited_requests.created_at :desc])
      (sql/left-join :users
                     [:= :audited_requests.user_id :users.id])
      (sql/left-join :audited_responses
                     [:or
                      [:= :audited_requests.txid :audited_responses.txid]
                      [:= :audited_requests.tx2id :audited_responses.tx2id]])))

(defn filter-by-user-uid [query {{user-uid :user-uid} :query-params}]
  (if-let [user-uid (presence user-uid)]
    (sql/where
     query
     [:exists
      (-> (sql/select :true)
          (sql/from :users)
          (sql-where-unique-user user-uid)
          (sql/where [:= :users.id :audited_requests.user_id]))])
    query))

(defn filter-by-method [query {{method :method} :query-params}]
  (if-let [method (some-> method presence string/lower-case)]
    (sql/where query [:= :audited_requests.method method])
    query))

(defn audited-requests [{tx-next :tx-next :as request}]
  {:body {:requests
          (-> requests-base-query
              (filter-by-user-uid request)
              (filter-by-method request)
              (set-per-page-and-offset request)
              sql-format
              (->> (jdbc/execute! tx-next)))}})

(defn routes [request]
  (case (:request-method request)
    :get (-> request
             (mixin-default-query-params default-query-params)
             audited-requests)))

;#### debug ###################################################################
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/wrap-with-log-debug #'users-formated-query)
