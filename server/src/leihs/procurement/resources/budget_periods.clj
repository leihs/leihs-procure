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
  (spy (-> (jdbc/execute! tx (-> (sql/delete-from :procurement_budget_periods :pbp)
                                 (sql/where [:not-in :pbp.id (cast-uuids ids)])
                                 ;(sql/returning :*)
                                 sql-format
                                 spy
                                 )
                          ) :update-count))
  )


(defn set-date [d]
  d
  )

(defn update-budget-periods!
  [context args value]

  (println ">oo> update-budget-periods!")
(spy   (let [tx (-> context
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
                                ;(update :end_date time-format/parse))
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