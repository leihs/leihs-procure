(ns leihs.procurement.resources.budget-periods
  (:require [clj-time.format :as time-format]
    ;[clojure.java.jdbc :as jdbc]
    ;[leihs.procurement.utils.sql :as sql]

            [honey.sql :refer [format] :rename {format sql-format}]
            [honey.sql.helpers :as sql]
            [leihs.core.db :as db]

            [logbug.debug :as debug]

            [leihs.procurement.utils.helpers :refer [add-comment-to-sql-format cast-uuids]]

            [leihs.procurement.resources.budget-period :as budget-period]

            [java-time :as jt]
            [next.jdbc :as jdbc]
            [taoensso.timbre :refer [debug error info spy warn]]

            )

  )



;(defn parse-date-string [date-str]
;  (jt/zoned-date-time date-str "yyyy-MM-dd HH:mm:ss.S"))



(def budget-periods-base-query
  (-> (sql/select :*)
      (sql/from :procurement_budget_periods)
      (sql/order-by [:end_date :desc])))

(defn budget-periods-query
  [args]

  (spy (let [
             ids (spy (:id args))
             ]
         (cond-> budget-periods-base-query
                 ids (sql/where [:in :procurement_budget_periods.id (cast-uuids ids)])
                 (spy (-> args
                          :whereRequestsCanBeMovedTo
                          empty?
                          not)) (sql/where [:< :current_date :procurement_budget_periods.end_date]))
         ))

  )

(defn get-budget-periods
  ([tx ids]
   (println ">oo> Causes issues1 >>" ids)
   (spy (jdbc/execute! tx (-> budget-periods-base-query
                              (sql/where [:in :procurement_budget_periods.id (ids)]) ;;TODO: FIXME
                              sql-format
                              spy
                              )))
   )

  ([context args _]
   (println ">oo> Causes issues2 >>" args)
   ;(if (spy (= (:id args) nil))
   (if (spy (= (:id args) []))
     []
     (spy (jdbc/execute! (-> context
                             :request
                             :tx-next) (-> (spy args)
                                           budget-periods-query
                                           sql-format
                                           spy))))

   ))

;(->> args
;     budget-periods-query
;     sql-format
;     (jdbc/execute! (-> context
;                        :request
;                        :tx-next))))))

;(defn cast-uuids [uuids]
;  (map (fn [uuid-str] [:cast uuid-str :uuid]) uuids))

(defn delete-budget-periods-not-in!
  [tx ids]

  (println ">o> tocheck delete-budget-periods-not-in! ???" ids)
  (spy (-> (spy (jdbc/execute-one! tx (-> (sql/delete-from :procurement_budget_periods :pbp)
                                 (sql/where [:not-in :pbp.id (cast-uuids ids)])
                                 ;(sql/returning :*)
                                 sql-format
                                 spy
                                 )
                          ))
           ;:update-count
           :next.jdbc/update-count
           list
           ))
  )


(defn set-date [d]
  d
  )

(defn update-budget-periods!
  [context args value]

  (println ">oo> update-budget-periods!")
  (spy (let [tx (-> context
                    :request
                    :tx-next)
             bps (:input_data args)]
         (spy (loop [[bp & rest-bps] bps
                     bp-ids []]
                (if bp
                  (let [

                        p (println ">oo> before bp-with-dates=" bp)
                        bp-with-dates (spy (-> bp
                                               ;(update :inspection_start_date time-format/parse) ;; TODO: fix parsing of timestamp
                                               ;(update :end_date time-format/parse)))
                                               (update :inspection_start_date set-date) ;; TODO: fix parsing of timestamp
                                               (update :end_date set-date)))
                        p (println ">oo> after bp-with-dates=" bp-with-dates)
                        ]
                    (do
                      (if (spy (:id bp-with-dates))
                        (spy (budget-period/update-budget-period! tx (spy bp-with-dates))) ;; is nil, should be 1
                        (spy (budget-period/insert-budget-period! tx (dissoc bp-with-dates :id))))

                      (let [
                            p (println ">oo> superorsch args=" args)
                            p (println ">oo> superorsch value=" value)
                            bp-id (or (spy (:id bp-with-dates))
                                      (spy (-> bp-with-dates
                                               (dissoc :id)
                                               (->> (budget-period/get-budget-period tx))
                                               :id)))

                            ]
                        (recur rest-bps (conj bp-ids bp-id)))

                      ))
                  (spy (do (spy (delete-budget-periods-not-in! tx bp-ids))
                           (println ">>> ??? delete _> get-budget-periods" args value)
                           (spy (get-budget-periods context args value))

                           ; TODO FIXME
                           ;nil
                           ;
                           ;expected: {"data"=>{"budget_periods"=>[{"name"=>"new_bp"}, {"name"=>"bp_1_new_name"}]}}
                           ;got: {"data"=>{"budget_periods"=>nil}}

                           ))

                  )))

         ))

  )





;(defn parse-date-string [input]
;  (let [date-str (second (re-find #":inspection_start_date \"([^\"]+)\"" input))]
;    (jt/zoned-date-time date-str "yyyy-MM-dd HH:mm:ss.S")))



(comment

  (let [

        tx (db/get-ds-next)


        date {:id #uuid "aba0576e-d65f-5fe0-aa80-89ce226ec9b1", :name 2017, :inspection_start_date "2016-11-04 01:00:00.0", :end_date "2017-01-22 01:00:00.0"}

        insp-date-str (:inspection_start_date date)
        ;insp-date-str (parse-date-string (:inspection_start_date date))
        p (println ">o> insp-date-str" insp-date-str)

        end-date-str (:end_date date)
        ;end-date-str (parse-date-string (:end_date date))
        p (println ">o> end-date-str" end-date-str)

        ;result (date-time-parse date-str)
        ;p (println ">o> result=" result)

        query (-> (sql/insert-into :procurement_budget_periods)

                  ;(sql/values [{:name "abc" :insp-date-str [:cast insp-date-str :timestamp]
                  ;              :end-date [:cast end-date-str :timestamp]
                  ;              }])

                  (sql/values [{:name "abc6"
                                :inspection_start_date [:cast insp-date-str :timestamp]
                                :end-date [:cast end-date-str :timestamp]
                                }])
                  sql-format
                  spy

                  )
        p (println ">o> query=" query)

        result (jdbc/execute-one! tx query)
        p (println ">o> result/update-count=" result)
        p (println ">o> result/update-count=" (:next.jdbc/update-count result))
        ])

  )

;#### debug ###################################################################
(debug/debug-ns *ns*)

;2023-12-17T06:48:47.621Z NX-41294 DEBUG [leihs.procurement.resources.budget-period:39] - (first (spy (jdbc/query tx (-> budget-period-base-query (sql/merge-where where-clause) sql/format)))) => {:id #uuid "80f2897a-dc35-4ecf-a997-b744e79a478c", :name "new_bp", :inspection_start_date #time/instant "2025-06-01T00:00:00Z", :end_date #time/instant "2025-12-01T00:00:00Z", :created_at #time/instant "2023-12-17T06:48:47.361794Z", :updated_at #time/instant "2023-12-17T06:48:47.361794Z"}
;2023-12-17T06:48:47.765Z NX-41294 DEBUG [leihs.procurement.resources.budget-periods:39] - (cond-> budget-periods-base-query (:id args) (sql/merge-where [:in :procurement_budget_periods.id (:id args)]) (spy (-> args :whereRequestsCanBeMovedTo empty? not)) (sql/merge-where [:< :current_date :procurement_budget_periods.end_date])) => {:select (:procurement_budget_periods.*), :from (:procurement_budget_periods), :order-by ([:end_date :desc])}
;2023-12-17T06:48:47.769Z NX-41294 DEBUG [leihs.procurement.resources.budget-periods:60] - (if (spy (= (:id args) [])) (spy []) (spy (->> args budget-periods-query sql/format (jdbc/query (-> context :request :tx))))) => ({:id #uuid "80f2897a-dc35-4ecf-a997-b744e79a478c", :name "new_bp", :inspection_start_date #time/instant "2025-06-01T00:00:00Z", :end_date #time/instant "2025-12-01T00:00:00Z", :created_at #time/instant "2023-12-17T06:48:47.361794Z", :updated_at #time/instant "2023-12-17T06:48:47.361794Z"} {:id #uuid "ab45ea8b-c753-465f-9cb0-fd9933248f3e", :name "bp_1_new_name", :inspection_start_date #time/instant "2024-06-01T00:00:00Z", :end_date #time/instant "2024-12-01T00:00:00Z", :created_at #time/instant "2023-12-17T06:48:46.235266Z", :updated_at #time/instant "2023-12-17T06:48:46.235266Z"})
;2023-12-17T06:48:47.770Z NX-41294 DEBUG [leihs.procurement.resources.budget-periods:106] - (let [tx (-> context :request :tx) bps (:input_data args)] (loop [[bp & rest-bps] bps bp-ids []] (if (spy bp) (let [bp-with-dates (spy (-> bp (update :inspection_start_date time-format/parse) (update :end_date time-format/parse)))] (do (if (spy (:id bp-with-dates)) (spy (budget-period/update-budget-period! tx bp-with-dates)) (spy (budget-period/insert-budget-period! tx (dissoc bp-with-dates :id)))) (let [bp-id (or (spy (:id bp-with-dates)) (spy (-> bp-with-dates (dissoc :id) (->> (budget-period/get-budget-period tx)) :id)))] (recur rest-bps (conj bp-ids bp-id))))) (do (spy (delete-budget-periods-not-in! tx bp-ids)) (spy (get-budget-periods context args value)))))) => ({:id #uuid "80f2897a-dc35-4ecf-a997-b744e79a478c", :name "new_bp", :inspection_start_date #time/instant "2025-06-01T00:00:00Z", :end_date #time/instant "2025-12-01T00:00:00Z", :created_at #time/instant "2023-12-17T06:48:47.361794Z", :updated_at #time/instant "2023-12-17T06:48:47.361794Z"} {:id #uuid "ab45ea8b-c753-465f-9cb0-fd9933248f3e", :name "bp_1_new_name", :inspection_start_date #time/instant "2024-06-01T00:00:00Z", :end_date #time/instant "2024-12-01T00:00:00Z", :created_at #time/instant "2023-12-17T06:48:46.235266Z", :updated_at #time/instant "2023-12-17T06:48:46.235266Z"})
;2023-12-17T06:48:47.772Z NX-41294 DEBUG [leihs.procurement.graphql.resolver:19] - ((spy resolver) (spy context) (spy args) (spy value)) => ({:id #uuid "80f2897a-dc35-4ecf-a997-b744e79a478c", :name "new_bp", :inspection_start_date #time/instant "2025-06-01T00:00:00Z", :end_date #time/instant "2025-12-01T00:00:00Z", :created_at #time/instant "2023-12-17T06:48:47.361794Z", :updated_at #time/instant "2023-12-17T06:48:47.361794Z"} {:id #uuid "ab45ea8b-c753-465f-9cb0-fd9933248f3e", :name "bp_1_new_name", :inspection_start_date #time/instant "2024-06-01T00:00:00Z", :end_date #time/instant "2024-12-01T00:00:00Z", :created_at #time/instant "2023-12-17T06:48:46.235266Z", :updated_at #time/instant "2023-12-17T06:48:46.235266Z"})


;>>>{"data":{"budget_periods":[{"name":"new_bp"},{"name":"bp_1_new_name"}]}}
;>>1>{"name":"new_bp","inspection_start_date":"2025-06-01T00:00:00.000+00:00","end_date":"2025-12-01T00:00:00.000+00:00"}
;>>1>{"name":"bp_1_new_name","inspection_start_date":"2024-06-01T00:00:00.000+00:00","end_date":"2024-12-01T00:00:00.000+00:00"}
;>>2>{"budget_period":{"name":"bp_1_new_name"}}
;>>3>{"name":"bp_1_new_name"}
;>>4>f90ee088-d13f-4f9d-a210-33262ec26493
;
;
;expected: {"data"=>{"budget_periods"=>[{"name"=>"new_bp"}, {"name"=>"bp_1_new_name"}]}}
;got: {"data"=>{"budget_periods"=>[{"name"=>"new_bp"}, {"name"=>"bp_1"}]}}
