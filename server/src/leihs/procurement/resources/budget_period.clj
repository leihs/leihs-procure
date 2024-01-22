(ns leihs.procurement.resources.budget-period
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.procurement.utils.helpers :refer [to-uuid]]
   [leihs.procurement.utils.sql :as sqlp]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]
   [tick.core :as tick]))

(def budget-period-base-query
  (-> (sql/select :procurement_budget_periods.*)
      (sql/from :procurement_budget_periods)))

(defn get-budget-period-by-id
  [tx id]
  (jdbc/execute-one! tx (-> budget-period-base-query
                            (sql/where [:= :procurement_budget_periods.id (to-uuid id)])
                            sql-format)))

(defn get-budget-period
  ([context _ value]
   (get-budget-period-by-id (-> context
                                :request
                                :tx-next)
                            (or (:budget_period_id value)
                                ; for BudgetLimit
                                (:value value)
                                ; for RequestFieldBudgetPeriod
                                )))
  ([tx bp-map]
   (jdbc/execute-one! tx (-> budget-period-base-query
                             (sql/where (sqlp/map->where-clause :procurement_budget_periods
                                                                bp-map))
                             sql-format))))

(defn sql-format-date [inst]
  (-> inst tick/date str))

(defn in-requesting-phase?
  [tx budget-period]
  (let [query (-> (sql/select [(as-> budget-period <>
                                 (:inspection_start_date <>)
                                 (sql-format-date <>)
                                 [:cast <> :date]
                                 [:< :current_date <>]) :result])
                  sql-format)]
    (:result (->> query
                  (jdbc/execute-one! tx)))))

(defn in-inspection-phase?
  [tx budget-period]
  (let [inspection-start-date (as-> budget-period <>
                                (:inspection_start_date <>)
                                (sql-format-date <>)
                                [:cast <> :date])
        end-date (as-> budget-period <>
                   (:end_date <>)
                   (sql-format-date <>)
                   [:cast <> :date])
        query (-> (sql/select
                   [[:and
                     [:>= :current_date inspection-start-date]
                     [:< :current_date end-date]] :result])
                  sql-format)]
    (:result (jdbc/execute-one! tx query))))

(defn past?
  [tx budget-period]
  (:result (jdbc/execute-one! tx (-> (sql/select [(as-> budget-period <>
                                                    (:end_date <>)
                                                    (sql-format-date <>)
                                                    [:cast <> :date]
                                                    [:> :current_date <>]) :result])
                                     sql-format))))

(defn can-delete?
  [context _ value]
  (:result (-> (jdbc/execute-one! (-> context
                                      :request
                                      :tx-next) (-> [:and
                                                     [:not
                                                      [:exists
                                                       (-> (sql/select true)
                                                           (sql/from [:procurement_requests :pr])
                                                           (sql/where [:= :pr.budget_period_id (:id value)]))]]
                                                     [:not
                                                      [:exists
                                                       (-> (sql/select true)
                                                           (sql/from [:procurement_budget_limits :pbl])
                                                           (sql/where [:= :pbl.budget_period_id (:id value)]))]]]
                                                    (vector :result)
                                                    sql/select
                                                    sql-format)))))

(defn update-budget-period!
  [tx bp]
  (jdbc/execute-one! tx (-> (sql/update :procurement_budget_periods)
                            (sql/set bp)
                            (sql/where [:= :procurement_budget_periods.id (:id bp)])
                            sql-format)))

(defn insert-budget-period!
  [tx bp]
  (jdbc/execute-one! tx (-> (sql/insert-into :procurement_budget_periods)
                            (sql/values [bp])
                            sql-format)))

;#### debug ###################################################################
;(debug/debug-ns *ns*)

