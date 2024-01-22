(ns leihs.procurement.resources.budget-periods
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.procurement.resources.budget-period :as budget-period]
   [leihs.procurement.utils.helpers :refer [convert-dates]]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]))

(defn insert-test-period-budget [tx data]
  (jdbc/execute-one! tx (-> (sql/insert-into :procurement_budget_periods)
                            (sql/values [data])
                            sql-format)))

(def budget-periods-base-query
  (-> (sql/select :*)
      (sql/from :procurement_budget_periods)
      (sql/order-by [:end_date :desc])))

(defn budget-periods-query
  [args]
  (let [ids (:id args)
        result (cond-> budget-periods-base-query
                 ids (sql/where [:in :procurement_budget_periods.id ids])
                 (-> args
                     :whereRequestsCanBeMovedTo
                     empty?
                     not) (sql/where [:< :current_date :procurement_budget_periods.end_date]))]
    result))

(defn get-budget-periods
  ([tx ids]
   (jdbc/execute! tx (-> budget-periods-base-query
                         (sql/where [:in :procurement_budget_periods.id (ids)])
                         sql-format)))

  ([context args _]
   (if (= (:id args) [])
     []
     (map convert-dates (jdbc/execute! (-> context
                                           :request
                                           :tx-next) (-> args
                                                         budget-periods-query
                                                         sql-format))))))

(defn delete-budget-periods-not-in!
  [tx ids]
  (jdbc/execute-one! tx (-> (sql/delete-from :procurement_budget_periods :pbp)
                            (sql/where [:not-in :pbp.id ids])
                            sql-format)))

(defn update-budget-periods!
  [context args value]
  (let [tx (-> context
               :request
               :tx-next)
        bps (:input_data args)
        result (loop [[bp & rest-bps] bps
                      bp-ids []]
                 (if bp
                   (let [bp-with-dates bp]
                     (do
                       (if (:id bp-with-dates)
                         (budget-period/update-budget-period! tx bp-with-dates)
                         (budget-period/insert-budget-period! tx (dissoc bp-with-dates :id)))
                       (let [bp-id (or (:id bp-with-dates)
                                       (-> bp-with-dates
                                           (dissoc :id)
                                           (->> (budget-period/get-budget-period tx))
                                           :id))]
                         (recur rest-bps (conj bp-ids bp-id)))))
                   (do (delete-budget-periods-not-in! tx bp-ids)
                       (get-budget-periods context args value))))]
    result))
