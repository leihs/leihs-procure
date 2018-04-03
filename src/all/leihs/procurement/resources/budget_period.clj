(ns leihs.procurement.resources.budget-period
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]
            ))

(def budget-period-id "aba0576e-d65f-5fe0-aa80-89ce226ec9b1")

(defn budget-period-query [id]
  (-> (sql/select :*)
      (sql/from :procurement_budget_periods)
      (sql/where [:= :procurement_budget_periods.id id])
      sql/format))

(defn get-budget-period [{tx :tx} id]
  (first (jdbc/query tx (budget-period-query id))))

(defn in-requesting-phase? [{tx :tx} budget-period]
  (:result
    (first
      (jdbc/query
        tx
        (-> (sql/select
              [(sql/call :<
                         (sql/call :cast (sql/call :now) :date)
                         (sql/call :cast (:inspection_start_date budget-period) :date))
               :result])
            sql/format)))))

(defn past? [{tx :tx} budget-period]
  (:result
    (first
      (jdbc/query
        tx
        (-> (sql/select
              [(sql/call :>
                         (sql/call :cast (sql/call :now) :date)
                         (sql/call :cast (:end_date budget-period) :date))
               :result])
            sql/format)))))
