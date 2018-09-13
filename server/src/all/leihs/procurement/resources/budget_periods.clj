(ns leihs.procurement.resources.budget-periods
  (:require [clojure.tools.logging :as log]
            [clj-time.format :as time-format]
            [clojure.java.jdbc :as jdbc]
            [com.walmartlabs.lacinia [resolve :as resolve]]
            [leihs.procurement.resources.budget-period :as budget-period]
            [leihs.procurement.utils.sql :as sql]))

(def budget-periods-base-query
  (-> (sql/select :procurement_budget_periods.*)
      (sql/from :procurement_budget_periods)
      (sql/order-by [:end_date :desc])))

(defn budget-periods-query
  [args]
  (cond-> budget-periods-base-query
    (:id args) (sql/merge-where [:in :procurement_budget_periods.id
                                 (:id args)])
    (-> args
        :whereRequestsCanBeMovedTo
        empty?
        not)
      (sql/merge-where [:> :current_date
                        :procurement_budget_periods.end_date])))

(defn get-budget-periods
  ([tx ids]
   (jdbc/query tx
               (-> budget-periods-base-query
                   (sql/merge-where [:in :procurement_budget_periods.id ids])
                   sql/format)))
  ([context args _]
   (if (= (:id args) [])
     []
     (->> args
          budget-periods-query
          sql/format
          (jdbc/query (-> context
                          :request
                          :tx))))))

(defn delete-budget-periods-not-in!
  [tx ids]
  (jdbc/execute! tx
                 (-> (sql/delete-from [:procurement_budget_periods :pbp])
                     (sql/merge-where [:not-in :pbp.id ids])
                     sql/format)))

(defn update-budget-periods!
  [context args value]
  (let [tx (-> context
               :request
               :tx)
        bps (:input_data args)]
    (loop [[bp & rest-bps] bps
           bp-ids []]
      (if bp
        (let [bp-with-dates (-> bp
                                (update :inspection_start_date
                                        time-format/parse)
                                (update :end_date time-format/parse))]
          (do
            (if (:id bp-with-dates)
              (budget-period/update-budget-period! tx bp-with-dates)
              (budget-period/insert-budget-period! tx
                                                   (dissoc bp-with-dates :id)))
            (let [bp-id (or (:id bp-with-dates)
                            (-> bp-with-dates
                                (dissoc :id)
                                (->> (budget-period/get-budget-period tx))
                                :id))]
              (recur rest-bps (conj bp-ids bp-id)))))
        (do (delete-budget-periods-not-in! tx bp-ids)
            (get-budget-periods context args value))))))
