(ns leihs.procurement.resources.budget-limits
  (:require
    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]

        [taoensso.timbre :refer [debug info warn error spy]]

        [leihs.core.utils :refer [my-cast]]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]
    ))

(def budget-limits-base-query
  (-> (sql/select :procurement_budget_limits.*)
      (sql/from :procurement_budget_limits)
      (sql/join :procurement_budget_periods
                [:= :procurement_budget_periods.id
                 :procurement_budget_limits.budget_period_id])
      (sql/order-by [:procurement_budget_periods.end_date :desc])))

(defn get-budget-limits
  [context _ value]
  (let [main_category_id (spy (:id value))]
    (spy (jdbc/execute!
      (-> context
          :request
          :tx-next)
      (-> budget-limits-base-query
          (sql/where [:= :procurement_budget_limits.main_category_id
                            main_category_id])
          sql-format)))))

(defn insert-budget-limit!
  [tx bl]
  (spy (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_budget_limits)
                     (sql/values [(my-cast bl)])
                     sql-format))))

(defn delete-budget-limit!
  [tx bl]
  (spy (jdbc/execute!
    tx
    ;(-> (sql/delete-from [:procurement_budget_limits :pbl])
    (-> (sql/delete-from :procurement_budget_limits :pbl)
        (sql/where [:and [:= :pbl.main_category_id [:cast (:main_category_id bl) :uuid]]
                    [:= :pbl.budget_period_id [:cast (:budget_period_id bl) :uuid]]])
        sql-format))))

(defn update-budget-limits!
  [tx bls]
  (doseq [bl bls]
    (spy (delete-budget-limit! tx bl))
    (spy (insert-budget-limit! tx bl))))
