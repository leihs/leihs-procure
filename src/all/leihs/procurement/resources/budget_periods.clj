(ns leihs.procurement.resources.budget-periods
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(def budget-periods-base-query
  (-> (sql/select :procurement_budget_periods.*)
      (sql/from :procurement_budget_periods)))

(defn get-budget-periods [context _ _]
  (jdbc/query (-> context :request :tx)
              (sql/format budget-periods-base-query)))
