(ns leihs.procurement.resources.budget-period
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]
            ))

(defn budget-period-query [id]
  (-> (sql/select :*)
      (sql/from :procurement_budget_periods)
      (sql/where [:= :procurement_budget_periods.id id])
      sql/format))

(defn get-budget-period [context _ value]
  (first (jdbc/query (-> context :request :tx)
                     (budget-period-query (:budget_period_id value)))))

(defn get-budget-period-by-id [tx id]
  (first (jdbc/query tx (budget-period-query id))))

(defn in-requesting-phase? [tx budget-period]
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

(defn past? [tx budget-period]
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

(defn can-delete? [context _ value]
  (-> (jdbc/query
        (-> context :request :tx)
        (-> (sql/call
              :and
              (sql/call
                :not
                (sql/call
                  :exists
                  (-> (sql/select true)
                      (sql/from [:procurement_requests :pr])
                      (sql/merge-where [:= :pr.budget_period_id (:id value)]))))
              (sql/call
                :not
                (sql/call
                  :exists
                  (-> (sql/select true)
                      (sql/from [:procurement_budget_limits :pbl])
                      (sql/merge-where [:= :pbl.budget_period_id (:id value)]))))) 
            (vector :result)
            sql/select
            sql/format
            ))
      first
      :result))
