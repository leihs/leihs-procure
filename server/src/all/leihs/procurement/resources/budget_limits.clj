(ns leihs.procurement.resources.budget-limits
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(def budget-limits-base-query
  (-> (sql/select :procurement_budget_limits.*)
      (sql/from :procurement_budget_limits)
      (sql/join :procurement_budget_periods
                [:= :procurement_budget_periods.id
                 :procurement_budget_limits.budget_period_id])
      (sql/order-by [:procurement_budget_periods.end_date :desc])))

(defn get-budget-limits
  [context _ value]
  (let [main_category_id (:id value)]
    (jdbc/query
      (-> context
          :request
          :tx)
      (-> budget-limits-base-query
          (sql/merge-where [:= :procurement_budget_limits.main_category_id
                            main_category_id])
          sql/format))))

(defn insert-budget-limit!
  [tx bl]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_budget_limits)
                     (sql/values [bl])
                     sql/format)))

(defn delete-budget-limit!
  [tx bl]
  (jdbc/execute!
    tx
    (-> (sql/delete-from [:procurement_budget_limits :pbl])
        (sql/where [:and [:= :pbl.main_category_id (:main_category_id bl)]
                    [:= :pbl.budget_period_id (:budget_period_id bl)]])
        sql/format)))

(defn delete-budget-limits-not-in-main-category-ids!
  [tx ids]
  (jdbc/execute!
    tx
    (-> (sql/delete-from :procurement_budget_limits)
        (sql/where [:not-in :procurement_budget_limits.main_category_id ids])
        sql/format)))

(defn update-budget-limits!
  [tx bls]
  (doseq [bl bls]
    (delete-budget-limit! tx bl)
    (insert-budget-limit! tx bl)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
