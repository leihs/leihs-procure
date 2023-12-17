(ns leihs.procurement.resources.budget-period
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.tools.logging :as log]
    [leihs.procurement.utils.sql :as sql]

        [taoensso.timbre :refer [debug info warn error spy]]

    [logbug.debug :as debug]
    [leihs.core.db :as db]

    [tick.core :as tick]
    ))

(def budget-period-base-query
  (-> (sql/select :procurement_budget_periods.*)
      (sql/from :procurement_budget_periods)))

(defn get-budget-period-by-id
  [tx id]
  (spy (first (spy (jdbc/query tx
                     (-> budget-period-base-query
                         (sql/where [:= :procurement_budget_periods.id id])
                         sql/format))))))

(defn get-budget-period
  ([context _ value]
   (spy (get-budget-period-by-id (-> context
                                :request
                                :tx)
                            (or (:budget_period_id value)
                                ; for BudgetLimit
                                (:value value)
                                ; for RequestFieldBudgetPeriod
                                ))))
  ([tx bp-map]
   (let [where-clause (sql/map->where-clause :procurement_budget_periods
                                             bp-map)]
     (spy (first (spy (jdbc/query tx
                        (-> budget-period-base-query
                            (sql/merge-where where-clause)
                            sql/format))))))))

(defn sql-format-date [inst]
  (-> inst tick/date str))

(defn in-requesting-phase?
  [tx budget-period]
  (let [query (-> (sql/select [(as-> budget-period <>
                                 (:inspection_start_date <>)
                                 (sql-format-date <>)
                                 (sql/call :cast <> :date)
                                 (sql/call :< :current_date <>)) :result])
                  sql/format)]
    (spy (->> query
         (jdbc/query tx)
         first
         :result))))

(defn in-inspection-phase?
  [tx budget-period]
  (let [inspection-start-date (as-> budget-period <>
                                (:inspection_start_date <>)
                                (sql-format-date <>)
                                (sql/call :cast <> :date))
        end-date (as-> budget-period <>
                   (:end_date <>)
                   (sql-format-date <>)
                   (sql/call :cast <> :date))
        query (->
                (sql/select
                  [(sql/call :and
                             (sql/call :>= :current_date inspection-start-date)
                             (sql/call :< :current_date end-date))
                   :result])
                sql/format)]
   (spy  (->> query
         (jdbc/query tx)
         first
         :result))))

(defn past?
  [tx budget-period]

  ;>oo> past? budget-period= {:id #uuid "71bd50a3-dfac-42b9-bf55-60bd151c2556", :name BP-in-inspection-phase,
  ; :inspection_start_date #time/instant "2023-12-05T23:00:00Z", :end_date #time/instant "2024-01-06T23:00:00Z",
  ; :created_at #time/instant "2023-12-07T10:39:01.374390Z", :updated_at #time/instant "2023-12-07T10:39:01.374390Z"}

  ;>o> past? query= [SELECT current_date > CAST(? AS date) AS result  2024-01-07]
  ;>o> past? result= false

  (println ">oo> past? budget-period=" budget-period)

  (let [query (-> (sql/select [(as-> budget-period <>
                                 (:end_date <>)
                                 (sql-format-date <>)
                                 (sql/call :cast <> :date)
                                 (sql/call :> :current_date <>)) :result])
                  sql/format)
        p (println ">o> past? query=" query)

        result (->> query
                    (jdbc/query tx)
                    first
                    )


        p (println ">o> past? result=" result)

        ]
    (spy (:result (spy result)))

    )
  )





(comment
  (let [
        tx (db/get-ds)
        data {:end_date #time/instant "2024-01-06T23:00:00Z"}
        data {:end_date #time/instant "2023-01-06T23:00:00Z"}

        ;; [SELECT current_date > CAST(? AS date) AS result
        x (past? tx data)
        ]
    )
  )


(defn can-delete?
  [context _ value]
  (spy (->
         (spy (jdbc/query
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
                    sql/format)))
         first
         :result)))

(defn update-budget-period!
  [tx bp]
  (spy (jdbc/execute! tx
                      (-> (sql/update :procurement_budget_periods)
                          (sql/sset bp)
                          (sql/where [:= :procurement_budget_periods.id (:id bp)])
                          sql/format))))

(defn insert-budget-period!
  [tx bp]
  (spy (jdbc/execute! tx
                      (-> (sql/insert-into :procurement_budget_periods)
                          (sql/values [bp])
                          sql/format))))

;#### debug ###################################################################
(debug/debug-ns *ns*)

