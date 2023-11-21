(ns leihs.procurement.resources.budget-periods
  (:require
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [leihs.procurement.resources.budget-period :as budget-period]
    [leihs.procurement.utils.helpers :refer [convert-dates]]
    [leihs.procurement.utils.helpers :refer [cast-uuids]]
    [next.jdbc :as jdbc]
    [taoensso.timbre :refer [debug error info spy warn]]))


(defn insert-test-period-budget [tx data]
  (let [query (-> (sql/insert-into :procurement_budget_periods)
                  (sql/values [data])
                  sql-format)]
    (jdbc/execute-one! tx query)))

;; TODO: remove
;(defn parse-utc-string [utc-string]
;  (OffsetDateTime/parse utc-string))

;(defn parse-and-format-offset-date-time [utc-string new-offset]
;  (let [parsed-date (OffsetDateTime/parse utc-string)
;        changed-offset (.withOffsetSameInstant parsed-date (ZoneOffset/of new-offset))
;        formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ssX")]
;    (.format changed-offset formatter)))



;(import [java.time ZonedDateTime]
;        [java.time.format DateTimeFormatter])
;
;(defn parse-utc-string [utc-string]
;  (ZonedDateTime/parse utc-string (DateTimeFormatter/ISO_OFFSET_DATE_TIME)))

(def budget-periods-base-query
  (-> (sql/select :*)
      (sql/from :procurement_budget_periods)
      (sql/order-by [:end_date :desc])))

(defn budget-periods-query
  [args]
  (let [ids (:id args)
        result (cond-> budget-periods-base-query
                       ids (sql/where [:in :procurement_budget_periods.id (cast-uuids ids)])
                       (-> args
                           :whereRequestsCanBeMovedTo
                           empty?
                           not) (sql/where [:< :current_date :procurement_budget_periods.end_date]))]
    result))

(defn get-budget-periods
  ([tx ids]
   (jdbc/execute! tx (-> budget-periods-base-query
                         (sql/where [:in :procurement_budget_periods.id (ids)])
                         sql-format
                         )))

  ([context args _]
   (if (= (:id args) [])
     []
     (let [result (jdbc/execute! (-> context
                                     :request
                                     :tx-next) (-> args
                                                   budget-periods-query
                                                   sql-format))]
       (map convert-dates result)))))


(defn delete-budget-periods-not-in!
  [tx ids]
  (-> (jdbc/execute-one! tx (-> (sql/delete-from :procurement_budget_periods :pbp)
                                (sql/where [:not-in :pbp.id (cast-uuids ids)])
                                sql-format))
      :next.jdbc/update-count
      list))

(defn update-budget-periods!
  [context args value]
  (let [tx (-> context
               :request
               :tx-next)
        bps (:input_data args)
        result (loop [[bp & rest-bps] bps
                      bp-ids []]
                 (if bp
                   (let [
                         ;;; TODO: FYI, not needed,
                         ;;>o> bp {:id 1da4d520-88fb-4fce-83c4-d30c0b19c1e0, :name bp_1_new_name, :inspection_start_date 2024-06-01T00:00:00+00:00, :end_date 2024-12-01T00:00:00+00:00}
                         ;p (println ">o> bp" bp)
                         ;bp-with-dates (-> bp
                         ;                  (update :inspection_start_date parse-utc-string)
                         ;                  (update :end_date parse-utc-string))
                         ;
                         ;;>o> bp-with-dates {:id 1da4d520-88fb-4fce-83c4-d30c0b19c1e0, :name bp_1_new_name, :inspection_start_date #time/zoned-date-time "2024-06-01T00:00Z", :end_date #time/zoned-date-time "2024-12-01T00:00Z"}
                         ;p (println ">o> bp-with-dates" bp-with-dates)


                         bp-with-dates bp]
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
                   (do
                     (delete-budget-periods-not-in! tx bp-ids)
                     (get-budget-periods context args value))
                   ))]
    result))

;(debug/debug-ns *ns*)
