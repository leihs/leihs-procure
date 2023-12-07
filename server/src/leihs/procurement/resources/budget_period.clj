(ns leihs.procurement.resources.budget-period
  (:require

    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [leihs.core.db :as db]

    [leihs.procurement.utils.sql :as sqlp]


    ;[clojure.java.jdbc :as jdbc]
    [next.jdbc :as jdbc]
    [taoensso.timbre :refer [debug error info spy warn]]
    [tick.core :as tick]
    ))

(def budget-period-base-query
  (-> (sql/select :procurement_budget_periods.*)
      (sql/from :procurement_budget_periods)))

(defn get-budget-period-by-id
  [tx id]
  (first (jdbc/execute! tx
                        (-> budget-period-base-query
                            ;(sql/where [:= :procurement_budget_periods.id (:cast id :uuid)])
                            (sql/where [:= :procurement_budget_periods.id [:cast id :uuid]])
                            sql-format))))

(defn get-budget-period
  ([context _ value]

   (println ">oo> hoi1")

   (get-budget-period-by-id (-> context
                                :request
                                :tx-next)
                            (or (:budget_period_id value)
                                ; for BudgetLimit
                                (:value value)
                                ; for RequestFieldBudgetPeriod
                                )))
  ([tx bp-map]
   (let [where-clause (sqlp/map->where-clause :procurement_budget_periods
                                              bp-map)]
     (first (jdbc/execute! tx
                           (-> budget-period-base-query
                               (sql/where where-clause)
                               sql-format))))))

(defn sql-format-date [inst]
  (-> inst tick/date str))

(defn in-requesting-phase?
  [tx budget-period]
  (let [query (-> (sql/select [(as-> budget-period <>
                                 (:inspection_start_date <>)
                                 (sql-format-date <>)
                                 [:cast <> :date]
                                 [:< :current_date <>]) :result])
                  sql-format)

        p (println ">oo> in-requesting-phase?" query)

        ]
    (->> query
         (jdbc/execute-one! tx)
         :result)))

(defn in-inspection-phase?
  [tx budget-period]
  (let [inspection-start-date (as-> budget-period <>
                                (:inspection_start_date <>)
                                (sql-format-date <>)
                                [:cast <> :date])
        end-date (as-> budget-period <>
                   (:end_date <>)
                   (sql-format-date <>)
                   ;( :cast <> :date))
                   [:cast <> :date]
                   )


        p (println ">oo> start/end" inspection-start-date end-date)
        query (->
                (sql/select
                  [[:and
                    [:>= :current_date inspection-start-date]
                    [:< :current_date end-date]
                    ]:result] )
                sql-format)]
    (->> query
         (jdbc/execute-one! tx)
         :result)))

(defn past?
  [tx budget-period]
  (println ">oo> past? budget-period=" budget-period)

  ;{:id #uuid "71bd50a3-dfac-42b9-bf55-60bd151c2556", :name BP-in-inspection-phase, :inspection_start_date #inst "2023-12-05T23:00:00.000000000-00:00",
  ; :end_date #inst "2024-01-06T23:00:00.000000000-00:00", :created_at #inst "2023-12-07T10:39:01.374390000-00:00", :updated_at #inst "2023-12-07T10:39:01.374390000-00:00"}

  (let [
        query (-> (sql/select [(as-> budget-period <>
                                 (:end_date <>)
                                 (sql-format-date <>)
                                 [:cast <> :date]
                                 [:> :current_date <>]) :result])

                  sql-format)

        p (println ">o> past? query=" query)
        p (println ">o> past? result=" (->> query
                                            (jdbc/execute-one! tx)))

        result (->> query
                    (jdbc/execute-one! tx)
                    :result)

        p (println ">o> past? result=" result)

        ]
    (->> query
         (jdbc/execute-one! tx)
         :result)))


(comment
  (let [
        tx (db/get-ds)
        ;data {:end_date #time/instant "2024-01-06T23:00:00Z"} ;;false
        data {:end_date #time/instant "2023-01-06T23:00:00Z"} ;;true

        data {:id #uuid "71bd50a3-dfac-42b9-bf55-60bd151c2556", :name "BP-in-inspection-phase", :inspection_start_date #inst "2023-12-05T23:00:00.000000000-00:00",
              :end_date #inst "2024-01-06T23:00:00.000000000-00:00", :created_at #inst "2023-12-07T10:39:01.374390000-00:00", :updated_at #inst "2023-12-07T10:39:01.374390000-00:00"}

        ;query= [SELECT CAST(? AS DATE) AS result 2023-01-07] ;;broken?

        ;; [SELECT current_date > CAST(? AS date) AS result
        ;x (past? tx data)

        p (println ">> past?" (past? tx data))
        p (println ">> in-inspection-phase?" (in-inspection-phase? tx data))
        p (println ">> in-requesting-phase?" (in-requesting-phase? tx data))
        ]
    ;(past? tx data)
    )
  )


(defn can-delete?
  [context _ value]
  (println ">oo> hoi2")

  (-> (spy (jdbc/execute-one! (-> context
                                  :request
                                  :tx-next) (-> (
                                                  :and
                                                  (:not
                                                    (:exists
                                                      (-> (sql/select true)
                                                          (sql/from [:procurement_requests :pr])
                                                          (sql/where [:= :pr.budget_period_id [:cast (:id value) :uuid]]))))
                                                  (:not
                                                    (:exists
                                                      (-> (sql/select true)
                                                          (sql/from [:procurement_budget_limits :pbl])
                                                          (sql/where [:= :pbl.budget_period_id [:cast (:id value) :uuid]])))))
                                                (vector :result)
                                                sql/select
                                                sql-format)))

      :result))

(defn update-budget-period!
  [tx bp]
  (jdbc/execute! tx
                 (-> (sql/update :procurement_budget_periods)
                     (sql/set bp)
                     (sql/where [:= :procurement_budget_periods.id [:cast (:id bp) :uuid]])
                     sql-format)))

(defn insert-budget-period!
  [tx bp]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_budget_periods)
                     (sql/values [bp])
                     sql-format)))

;#### debug ###################################################################
;(debug/debug-ns *ns*)

