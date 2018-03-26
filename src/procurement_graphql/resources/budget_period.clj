(ns procurement-graphql.resources.budget-period
  (:require [honeysql.core :as sql]
            [honeysql.helpers :refer :all :rename {update honey-update}]
            [clojure.java.jdbc :as jdbc]
            [procurement-graphql.db :as db])) 

(def budget-period-id "aba0576e-d65f-5fe0-aa80-89ce226ec9b1")

(defn budget-period-query [id]
  (-> (select :*)
      (from :procurement_budget_periods)
      (where [:= :procurement_budget_periods.id (sql/call :cast id :uuid)])
      sql/format))

(defn get-budget-period [id]
  (first (jdbc/query db/db (budget-period-query id))))

(defn in-requesting-phase? [budget-period]
  (:result
    (first
      (jdbc/query
        db/db
        (-> (select
              [(sql/call :<
                         (sql/call :cast (sql/call :now) :date)
                         (sql/call :cast (:inspection_start_date budget-period) :date))
               :result])
            sql/format)))))

(defn past? [budget-period]
  (:result
    (first
      (jdbc/query
        db/db
        (-> (select
              [(sql/call :>
                         (sql/call :cast (sql/call :now) :date)
                         (sql/call :cast (:end_date budget-period) :date))
               :result])
            sql/format)))))

(past? (get-budget-period budget-period-id))
