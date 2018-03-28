(ns leihs.procurement.resources.request
  (:require [leihs.procurement.utils.sql :as sql]
            [leihs.procurement.utils.ds :refer [get-ds]]
            [clojure.java.jdbc :as jdbc]))

(defn request-query [id]
  (-> (sql/select :*)
      (sql/from :procurement_requests)
      (sql/where [:= :procurement_requests.id (sql/call :cast id :uuid)])
      sql/format))

(defn get-request [id]
  (first (jdbc/query (get-ds) (request-query id))))

(defn requested-by-user? [request user]
  (= (:user_id request) (:id user)))
