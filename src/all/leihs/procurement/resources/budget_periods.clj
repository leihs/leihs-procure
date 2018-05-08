(ns leihs.procurement.resources.budget-periods
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.resources.budget-period :as budget-period]
            [leihs.procurement.utils.sql :as sql]))  

(def budget-periods-base-query
  (-> (sql/select :procurement_budget_periods.*)
      (sql/from :procurement_budget_periods)
      (sql/order-by [:name :desc])))

(defn get-budget-periods [context _ _]
  (jdbc/query (-> context :request :tx)
              (sql/format budget-periods-base-query)))

(defn delete-budget-periods-not-in! [tx ids]
  (jdbc/execute! tx
                 (-> (sql/delete-from [:procurement_budget_periods :pbp])
                     (sql/merge-where [:not-in :pbp.id ids])
                     sql/format)))

(defn update-budget-periods! [context args value]
  (let [tx (-> context :request :tx)
        bps (:input_data args)]
    (loop [[bp & rest-bps] bps bp-ids []]
      (if bp
        (do (if (:id bp)
              (budget-period/update-budget-period! tx bp)
              (budget-period/insert-budget-period! tx (dissoc bp :id)))
            (let [bp-id (or (:id bp)
                            (->> bp (budget-period/get-budget-period tx) :id))]
              (recur rest-bps (conj bp-ids bp-id))))
        (do (delete-budget-periods-not-in! tx (log/spy bp-ids))
            (get-budget-periods context args value))))))
