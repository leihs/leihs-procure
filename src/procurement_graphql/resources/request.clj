(ns procurement-graphql.resources.request
  (:require [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers]
            [clojure.java.jdbc :as jdbc]
            [procurement-graphql.db :as db]))

(defn request-query [id]
  (-> (select :*)
      (from :procurement_requests)
      (where [:= :procurement_requests.id (sql/call :cast id :uuid)])
      sql/format))

(defn get-request [id]
  (first (jdbc/query db/db (request-query id))))
