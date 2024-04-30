(ns leihs.procurement.resources.budget-limits
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]))

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
    (jdbc/execute!
     (-> context
         :request
         :tx)
     (-> budget-limits-base-query
         (sql/where [:= :procurement_budget_limits.main_category_id
                     main_category_id])
         sql-format))))

(defn insert-budget-limit!
  [tx bl]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_budget_limits)
                     (sql/values [bl])
                     sql-format)))

(defn delete-budget-limit!
  [tx bl]
  (jdbc/execute!
   tx
   (-> (sql/delete-from :procurement_budget_limits :pbl)
       (sql/where [:and [:= :pbl.main_category_id (:main_category_id bl)]
                   [:= :pbl.budget_period_id (:budget_period_id bl)]])
       sql-format)))

(defn update-budget-limits!
  [tx bls]
  (doseq [bl bls]
    (delete-budget-limit! tx bl)
    (insert-budget-limit! tx bl)))
