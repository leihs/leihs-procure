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

  (:import

    [java.time OffsetDateTime ZoneOffset ZonedDateTime ZoneId]
    [java.time.format DateTimeFormatter]


    ;[java.time.OffsetDateTime]

    ;[java.sql Timestamp]
    [java.time ZonedDateTime ZoneId ZoneOffset]
    [java.time.format DateTimeFormatter])
  )


(defn insert-test-period-budget [tx data]

  (let [
        ;query (-> (sql/upsert :procurement_budget_periods)
        query (-> (sql/insert-into :procurement_budget_periods)
                  (sql/values [data])
                  sql-format
                  spy
                  )
        p (println ">o> query=" query)

        result (jdbc/execute-one! tx query)
        p (println ">o> result/update-count=" result)
        ]
    result))

;(defn change-offset-and-convert [utc-string new-offset]
;  (let [parsed-date (parse-utc-string utc-string)
;        offset-date-time (.withZoneSameInstant parsed-date (ZoneId/ofOffset "UTC" (ZoneOffset/of new-offset)))]
;    (zoned-date-time-to-timestamp offset-date-time)))
;;
;;(defn change-offset-to-timestamp [zoned-date-time new-offset]
;;  (let [offset-dt (.toOffsetDateTime zoned-date-time)
;;        adjusted-dt (.withOffsetSameInstant offset-dt (ZoneOffset/of new-offset))]
;;    (Timestamp/from (.toInstant adjusted-dt))))


(defn parse-utc-string [utc-string]
  (OffsetDateTime/parse utc-string))
;
;(defn change-timezone-offset [offset-date-time new-offset]
;  (.withOffsetSameInstant offset-date-time (ZoneOffset/of new-offset)))


(import [java.time OffsetDateTime ZoneOffset])

(defn parse-and-change-offset [utc-string new-offset]
  (let [parsed-date (OffsetDateTime/parse utc-string)
        offset (ZoneOffset/of new-offset)]
    (.withOffsetSameInstant parsed-date offset)))


(defn parse-and-format-offset-date-time [utc-string new-offset]
  (let [parsed-date (OffsetDateTime/parse utc-string)
        changed-offset (.withOffsetSameInstant parsed-date (ZoneOffset/of new-offset))
        formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ssX")]
    (.format changed-offset formatter)))

(comment

  ;; FIXME: modify & save utc in budget_periods with utc
  (let [
        tx (db/get-ds-next)

        ;; BROKEN: MODIFY & SAVE
        utc-string "2024-06-01T00:00:00+00:00"
        new-offset "+02:00" ;; Change this to the desired new offset
        ;updated-date (parse-and-change-offset utc-string new-offset)
        updated-date (parse-and-format-offset-date-time utc-string new-offset)
        ;
        ;p (println ">o> updated-date >> " updated-date)
        ;
        x (insert-test-period-budget tx {:name "ba222221"
                                         :inspection_start_date [:cast (str parsed-date1) :timestamptz]
                                         ;:end-date [:cast (str updated-date) :timestamptz]
                                         :end-date [:cast updated-date :timestamptz]
                                         ;:end-date (str updated-date)
                                         })
        ]
    )                                                       ;; here
  )



(comment

  ;; save utc in budget_periods with utc
  (let [
        utc-string-0 "2024-06-01T00:00:00+00:00"
        utc-string+1 "2024-06-01T00:00:00+01:00"
        utc-string+4 "2024-06-01T00:00:00+04:00"
        utc-string+2 "2024-06-01T00:00:00+02:00"

        parsed-date1 (parse-utc-string utc-string+4)
        parsed-date2 (parse-utc-string utc-string+1)

        utc-string "2024-06-01T00:00:00+00:00"
        parsed-date (parse-utc-string utc-string)

        tx (db/get-ds-next)

        ;; works
        x (insert-test-period-budget tx {:name "as-string"
                                         :inspection_start_date [:cast utc-string-0 :timestamptz]
                                         :end-date [:cast utc-string-0 :timestamptz]
                                         })

        ;; works
        x (insert-test-period-budget tx {:name "parsed-string"
                                         :inspection_start_date [:cast (str parsed-date1) :timestamptz]
                                         :end-date [:cast (str parsed-date2) :timestamptz]
                                         })
        ])
  )


(import [java.time ZonedDateTime]
        [java.time.format DateTimeFormatter])

(defn parse-utc-string [utc-string]
  (ZonedDateTime/parse utc-string (DateTimeFormatter/ISO_OFFSET_DATE_TIME)))



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


;(defn parse-utc-string [utc-string]
;  (jt/zoned-date-time utc-string))

(defn update-budget-periods!
  [context args value]

  (println ">ooo> update-budget-periods!")
  (let [tx (-> context
               :request
               :tx-next)
        bps (:input_data args)

        result (loop [[bp & rest-bps] bps
                      bp-ids []]
                 (if bp
                   (let [
                         ;p (spy (">ooo> delete & get-budget-period"))


                         p (println ">ooo> before  ??? 1 bp-with-dates=" bp)
                         p (println ">ooo> before  ??? 2 bp-with-dates=" (:inspection_start_date bp))
                         ;p (println ">ooo> before  ??? 3 bp-with-dates=" (time-format/parse (:inspection_start_date bp)))
                         ;p (println ">ooo> before  ??? 4 bp-with-dates=" (time-format/parse (:end_date bp)))
                         p (println ">ooo> before  ??? 3 bp-with-dates=" (parse-utc-string (:inspection_start_date bp)))
                         p (println ">ooo> before  ??? 4 bp-with-dates=" (parse-utc-string (:end_date bp)))

                         bp-with-dates (spy (-> bp
                                                spy
                                                ;(update :inspection_start_date time-format/parse) ;; TODO: fix parsing of timestamp
                                                ;(update :end_date time-format/parse)))
                                                ;
                                                (update :inspection_start_date parse-utc-string) ;; TODO: fix parsing of timestamp
                                                (update :end_date parse-utc-string)))
                         ;(update :inspection_start_date set-date) ;; TODO: fix parsing of timestamp
                         ;(update :end_date set-date)))
                         p (println ">ooo> after bp-with-dates=" bp-with-dates)
                         bp-with-dates bp

                         ]
                     (do
                       (if (spy (:id bp-with-dates))
                         (spy (budget-period/update-budget-period! tx (spy bp-with-dates))) ;; is nil, should be 1
                         (spy (budget-period/insert-budget-period! tx (dissoc bp-with-dates :id))))

                       (let [
                             ;p (println ">oo> superorsch args=" args)
                             ;p (println ">oo> superorsch value=" value)
                             bp-id (or (spy (:id bp-with-dates))
                                       (spy (-> bp-with-dates
                                                (dissoc :id)
                                                (->> (budget-period/get-budget-period tx))
                                                :id)))
                             ;p (spy (">ooo> get-budget-period" bp-id))
                             ]
                         (recur rest-bps (conj bp-ids bp-id)))

                       ))
                   (do
                     ;(spy ("=> delete & get-budget-period"))
                     (spy (delete-budget-periods-not-in! tx bp-ids))
                     (spy (get-budget-periods context args value))
                     )

                   ))

        ]

    (spy result)

    ;{"budget_periods"[{"name"=>"new_bp"}, {"name"=>"bp_1_new_name"}]}
    ;{:budget_periods [{:name "new_bp"}, {:name "bp_1_new_name"}]}

    )
  ;[{:id #uuid "9ab7cd6d-2af6-4088-8316-f1264ff4811c", :name "new_bp", :inspection_start_date #time/instant "2025-06-01T00:00:00Z", :end_date #time/instant "2025-12-01T00:00:00Z", :created_at #time/instant "2023-12-17T08:05:04.551321Z", :updated_at #time/instant "2023-12-17T08:05:04.551321Z"} {:id #uuid "619c02a3-fa61-486a-8c06-69ab9b0c7e73", :name "bp_1_new_name", :inspection_start_date #time/instant "2024-06-01T00:00:00Z", :end_date #time/instant "2024-12-01T00:00:00Z", :created_at #time/instant "2023-12-17T08:05:03.344216Z", :updated_at #time/instant "2023-12-17T08:05:03.344216Z"}]
  ;[{:id #uuid "9ab7cd6d-2af6-4088-8316-f1264ff4811c", :name "new_bp", :inspection_start_date  "2025-06-01T00:00:00.000+00:00", :end_date "2025-12-01T00:00:00.000+00:00", :created_at "2023-12-17T13:43:05.524+01:00", :updated_at  "2023-12-17T13:43:05.524+01:00"}, {:id #uuid "9ab7cd6d-2af6-4088-8316-f1264ff4811c", :name "bp_1_new_name", :inspection_start_date  "2024-06-01T00:00:00.000+00:00", :end_date "2024-12-01T00:00:00.000+00:00", :created_at "2023-12-17T13:43:05.524+01:00", :updated_at  "2023-12-17T13:43:05.524+01:00"}]
  ;[{:id #uuid "9ab7cd6d-2af6-4088-8316-f1264ff4811c", :name "new_bp", :inspection_start_date  "2025-06-01T00:00:00.000+00:00", :end_date "2025-12-01T00:00:00.000+00:00"},
  ; {:id #uuid "9ab7cd6d-2af6-4088-8316-f1264ff4811c", :name "bp_1_new_name", :inspection_start_date  "2024-06-01T00:00:00.000+00:00", :end_date "2024-12-01T00:00:00.000+00:00"}]


  ;[{"id":"37db1046-1798-4908-a35f-0817b5226f5e","name":"bp_1_new_name","inspection_start_date":"2024-06-01T00:00:00.000+02:00","end_date":"2024-12-01T00:00:00.000+01:00","created_at":"2023-12-17T13:43:04.372+01:00","updated_at":"2023-12-17T13:43:04.372+01:00"},{"id":"cceeb336-9775-4ab6-9cd7-03ced76c2cb5","name":"new_bp","inspection_start_date":"2025-06-01T00:00:00.000+02:00","end_date":"2025-12-01T00:00:00.000+01:00","created_at":"2023-12-17T13:43:05.524+01:00","updated_at":"2023-12-17T13:43:05.524+01:00"}]

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

; MASTER
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
