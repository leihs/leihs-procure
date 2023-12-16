(ns leihs.procurement.resources.budget-periods
  (:require [clojure.tools.logging :as log]
            [clj-time.format :as time-format]
            [clojure.java.jdbc :as jdbc]


            [logbug.debug :as debug]

                [taoensso.timbre :refer [debug info warn error spy]]

            [com.walmartlabs.lacinia [resolve :as resolve]]
            [leihs.procurement.resources.budget-period :as budget-period]
            [leihs.procurement.utils.sql :as sql]))


















(def budget-periods-base-query
  (-> (sql/select :procurement_budget_periods.*)
      (sql/from :procurement_budget_periods)
      (sql/order-by [:end_date :desc])))

(defn budget-periods-query
  [args]
  (spy (cond-> budget-periods-base-query
          (:id args) (sql/merge-where [:in :procurement_budget_periods.id
                                       (:id args)])
          (spy (-> args
              :whereRequestsCanBeMovedTo
              empty?
              not))
          (sql/merge-where [:< :current_date
                            :procurement_budget_periods.end_date]))))





(defn get-budget-periods
  ([tx ids]
   (spy (jdbc/query tx
               (-> budget-periods-base-query
                   (sql/merge-where [:in :procurement_budget_periods.id ids])
                   sql/format))))
  ([context args _]
   (if (spy (= (:id args) []))
     []
     (spy (->> args
          budget-periods-query
          sql/format
          (jdbc/query (-> context
                          :request
                          :tx)))))))



















(defn delete-budget-periods-not-in!
  [tx ids]
  (spy (jdbc/execute! tx
                 (-> (sql/delete-from [:procurement_budget_periods :pbp])
                     (sql/merge-where [:not-in :pbp.id ids])
                     sql/format))))











(defn update-budget-periods!
  [context args value]
  (let [tx (-> context
               :request
               :tx)
        bps (:input_data args)]
    (loop [[bp & rest-bps] bps
           bp-ids []]
      (if (spy bp)
        (let [bp-with-dates  (spy (-> bp                    ;;  => {:id nil, :name "new_bp", :inspection_start_date #clj-time/date-time "2025-06-01T00:00:00.000Z", :end_date #clj-time/date-time "2025-12-01T00:00:00.000Z"}
                                (update :inspection_start_date
                                           time-format/parse)
                                (update :end_date time-format/parse)))]
          (do
            (if (spy (:id bp-with-dates))
              (spy (budget-period/update-budget-period! tx bp-with-dates)) ;; nil
              (spy (budget-period/insert-budget-period! tx (dissoc bp-with-dates :id)))) ;; => (1)
            (let [bp-id (or (:id bp-with-dates)
                            (-> bp-with-dates
                                (dissoc :id)
                                (->> (budget-period/get-budget-period tx))
                                :id))]
             (recur rest-bps (conj bp-ids bp-id))

              )))
        (do (spy (delete-budget-periods-not-in! tx bp-ids)) ;; (delete-budget-periods-not-in! tx bp-ids) => (1)
            (spy (get-budget-periods context args value))
            ;;({:id #uuid "bc517cb6-8efb-44a7-be05-032a309e4504", :name "new_bp", :inspection_start_date #time/instant "2025-06-01T00:00:00Z", :end_date #time/instant "2025-12-01T00:00:00Z", :created_at #time/instant "2023-12-16T08:50:52.351065Z", :updated_at #time/instant "2023-12-16T08:50:52.351065Z"} {:id #uuid "a50ba927-bc61-4403-a5e3-03fd9e38d16f", :name "bp_1_new_name", :inspection_start_date #time/instant "2024-06-01T00:00:00Z", :end_date #time/instant "2024-12-01T00:00:00Z", :created_at #time/instant "2023-12-16T08:50:50.322549Z", :updated_at #time/instant "2023-12-16T08:50:50.322549Z"})

            )))))


;#### debug ###################################################################
(debug/debug-ns *ns*)