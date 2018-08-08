(ns leihs.procurement.resources.budget-period
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(def budget-period-base-query
  (-> (sql/select :procurement_budget_periods.*)
      (sql/from :procurement_budget_periods)))

(defn get-budget-period-by-id
  [tx id]
  (first (jdbc/query tx
                     (-> budget-period-base-query
                         (sql/where [:= :procurement_budget_periods.id id])
                         sql/format))))

(defn get-budget-period
  ([context _ value]
   (get-budget-period-by-id (-> context
                                :request
                                :tx)
                            (or (:budget_period_id value) ; for BudgetLimit
                                (:value value) ; for RequestFieldBudgetPeriod
                              )))
  ([tx bp-map]
   (let [where-clause (sql/map->where-clause :procurement_budget_periods
                                             bp-map)]
     (first (jdbc/query tx
                        (-> budget-period-base-query
                            (sql/merge-where where-clause)
                            sql/format))))))

(defn in-requesting-phase?
  [tx budget-period]
  (:result
    (first (jdbc/query tx
                       (-> (sql/select [(sql/call :<
                                                  (sql/call :now)
                                                  (:inspection_start_date
                                                    budget-period)) :result])
                           sql/format)))))

(defn past?
  [tx budget-period]
  (:result
    (first
      (jdbc/query tx
                  (-> (sql/select
                        [(sql/call :> (sql/call :now) (:end_date budget-period))
                         :result])
                      sql/format)))))

(defn can-delete?
  [context _ value]
  (->
    (jdbc/query
      (-> context
          :request
          :tx)
      (-> (sql/call
            :and
            (sql/call :not
                      (sql/call :exists
                                (-> (sql/select true)
                                    (sql/from [:procurement_requests :pr])
                                    (sql/merge-where [:= :pr.budget_period_id
                                                      (:id value)]))))
            (sql/call :not
                      (sql/call :exists
                                (-> (sql/select true)
                                    (sql/from [:procurement_budget_limits :pbl])
                                    (sql/merge-where [:= :pbl.budget_period_id
                                                      (:id value)])))))
          (vector :result)
          sql/select
          sql/format))
    first
    :result))

(defn update-budget-period!
  [tx bp]
  (jdbc/execute! tx
                 (-> (sql/update :procurement_budget_periods)
                     (sql/sset bp)
                     (sql/where [:= :procurement_budget_periods.id (:id bp)])
                     sql/format)))

(defn insert-budget-period!
  [tx bp]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_budget_periods)
                     (sql/values [bp])
                     sql/format)))

; (->> budget-period-base-query
;      sql/format
;      (jdbc/query (ds/get-ds))
;      first
;      (in-requesting-phase? (ds/get-ds)))
