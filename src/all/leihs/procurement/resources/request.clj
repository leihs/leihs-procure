(ns leihs.procurement.resources.request
  (:require [leihs.procurement.utils.sql :as sql]
            [clojure.java.jdbc :as jdbc]
            [leihs.procurement.db :as db]))

(defn request-query [id]
  (-> (sql/select :*)
      (sql/from :procurement_requests)
      (sql/where [:= :procurement_requests.id (sql/call :cast id :uuid)])
      sql/format))

(defn get-request [id]
  (first (jdbc/query db/conn (request-query id))))

(defn requested-by-user? [request user]
  (= (:user_id request) (:id user)))
