(ns procurement-graphql.resources.budget-period
  (:require [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers]
            [clojure.java.jdbc :as jdbc]
            [procurement-graphql.db :as db])) 

(defn budget-period-query [id]
  (-> (select :*)
      (from :procurement_budget_periods)
      (where [:= :procurement_budget_periods.id (sql/call :cast id :uuid)])
      sql/format))

(defn get-budget-period [id]
  (first (jdbc/query db/db (budget-period-query id))))
