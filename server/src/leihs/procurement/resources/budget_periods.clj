(ns leihs.procurement.resources.budget-periods
  (:require [clojure.tools.logging :as log]
            [clj-time.format :as time-format]
    
            ;[clojure.java.jdbc :as jdbc]
            ;[leihs.procurement.utils.sql :as sql]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]
    
            [com.walmartlabs.lacinia [resolve :as resolve]]
            [leihs.procurement.resources.budget-period :as budget-period]
    ))

(def budget-periods-base-query
  (-> (sql/select :*)
      (sql/from :procurement_budget_periods)
      (sql/order-by [:end_date :desc])))

(defn budget-periods-query
  [args]
  (cond-> budget-periods-base-query
    (:id args) (sql/where [:in :procurement_budget_periods.id
                                 (:cast (:id args) :uuid)])
    (-> args
        :whereRequestsCanBeMovedTo
        empty?
        not)
      (sql/where [:< :current_date
                        :procurement_budget_periods.end_date])))

(defn get-budget-periods
  ([tx ids]
   (println ">oo> Causes issues >>" ids)
   (jdbc/execute! tx
               (-> budget-periods-base-query
                   (sql/where [:in :procurement_budget_periods.id ids]) ;;TODO: FIXME
                   sql-format)))
  ([context args _]
   (if (= (:id args) [])
     []
     (->> args
          budget-periods-query
          sql-format
          (jdbc/execute! (-> context
                          :request
                          :tx-next))))))

(defn delete-budget-periods-not-in!
  [tx ids]
  (jdbc/execute! tx
                 (-> (sql/delete-from [:procurement_budget_periods :pbp])
                     (sql/where [:not-in :pbp.id ids])
                     sql-format)))

(defn update-budget-periods!
  [context args value]

  (println ">oo> update-budget-periods!")
  (let [tx (-> context
               :request
               :tx-next)
        bps (:input_data args)]
    (loop [[bp & rest-bps] bps
           bp-ids []]
      (if bp
        (let [bp-with-dates (-> bp
                                (update :inspection_start_date
                                        time-format/parse)
                                (update :end_date time-format/parse))
              p (println ">oo> bp-with-dates=" bp-with-dates)
              ]
          (do


            (if (:id bp-with-dates)
              (budget-period/update-budget-period! tx bp-with-dates)
              (budget-period/insert-budget-period! tx
                                                   (dissoc bp-with-dates :id)))

            (let [
                  p (println ">oo> superorsch args=" args)
                  p (println ">oo> superorsch value=" value)
                  bp-id (or (:id bp-with-dates)
                            (-> bp-with-dates
                                (dissoc :id)
                                (->> (budget-period/get-budget-period tx))
                                :id))

                  ]
              (recur rest-bps (conj bp-ids bp-id)))))
        (do (delete-budget-periods-not-in! tx bp-ids)
            (get-budget-periods context args value))))))
