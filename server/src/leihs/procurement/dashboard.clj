(ns leihs.procurement.dashboard
  (:require [clojure.string :as string]
            [clojure.data.json :as json]

            [taoensso.timbre :refer [debug info warn error spy]]



    ;[clojure.java.jdbc :as jdbc]
    ;[leihs.procurement.utils.sql :as sql]

            [honey.sql :refer [format] :rename {format sql-format}]
            [leihs.core.db :as db]
            [next.jdbc :as jdbc]
            [honey.sql.helpers :as sql]

            [clojure.tools.logging :as log]
            [leihs.procurement.resources.budget-periods :as budget-periods]
            [leihs.procurement.resources.categories :as categories]
            [leihs.procurement.resources.main-categories :as main-categories]
            [leihs.procurement.resources.requests :as requests]))

(defn sum-total-price [coll]

  (println ">oo>" "1sum-total-price :total_price_cents" coll)
  (println ">oo>" "2sum-total-price :total_price_cents" (->> coll
                                                             (map :total_price_cents)))

  (let [

        result (->> coll
                    (map :total_price_cents)
                    (map #(if (nil? %) 0 %))                ;;TODO remove this, SET DEFAULT FOR PRICES
                    (reduce +))

        p (println ">>1" result)

        ]
    result
    )
  )


(comment

  (let [
        ss [{:total_price_cents 1000}
            {:total_price_cents 2000}
            {:total_price_cents nil}
            {:total_price_cents 1500}]

        ;ss [{}]

        p (println ">oo>" (sum-total-price ss))

        ])
  )


(defn cache-key
  [& args]
  (->> args
       (map :id)
       (string/join "_")))


(defn filter-and-assoc-cats [mc bp requests dashboard-cache-key tx]

  (println ">o> filter-and-assoc-cats" requests)

  (->> mc
       :id
       (categories/get-for-main-category-id tx)
       (map (fn [c]
              (println ">o> c" c)
              (let [requests* (filter #(and (= (-> %
                                                   :category
                                                   :value
                                                   :id)
                                               (str (:id c)))
                                            (= (-> %
                                                   :budget_period
                                                   :value
                                                   :id)
                                               (str (:id bp))))
                                      requests)
                    p (println ">o> c" c)
                    p (println ">o> 2 requests*" requests*)

                    result (-> c
                               (assoc :requests requests*)
                               (assoc :total_price_cents (sum-total-price requests*))
                               (assoc :cacheKey
                                      (cache-key
                                        dashboard-cache-key
                                        bp
                                        mc
                                        c)))
                    p (println "\n>o> result 1a" result "\n")
                    ]

                ;[clojure.data.json :as json]
                ;p (println "\n>o> filter-and-assoc-cats (json)" (json/write-str result) "\n")
                ;p (println "\n>o> filter-and-assoc-cats -> :budget_period" (-> result :requests first :budget_period) "\n")
                ;p (println "\n>o> filter-and-assoc-cats -> :budget_period (json)" (json/write-str (-> result :requests first :budget_period)) "\n")
                result

                )))))

(defn printer



  ([res]
   (println "\n>o> final-result" res "\n")
   (println "\n>o> final-result (json)" (json/write-str res) "\n")

   res)

  ([title res]
   (println "\n>request-http _> " title res "\n")
   ;(println "\n>o> final-result (json)" (json/write-str res) "\n")

   res)

  )

(defn determine-budget-periods [requests tx dashboard-cache-key main-cats bps]
  (->> bps
       (map (fn [bp]
              (println ">o> bp" bp)
              (let [main-cats* (->>
                                 main-cats
                                 (map
                                   (fn [mc]
                                     (println ">o> mc" mc)
                                     (let [cats* (filter-and-assoc-cats mc bp requests dashboard-cache-key tx)
                                           ;p (println "\n>o> result 2a" cats* "\n")

                                           result (-> mc
                                                      (assoc :categories cats*)
                                                      (assoc :total_price_cents (sum-total-price cats*))
                                                      (assoc :cacheKey (cache-key dashboard-cache-key bp mc))
                                                      (->> (main-categories/merge-image-path tx)))

                                           p (println "\n>>ToCheck dashboard::determine-budget-periods" result "\n")

                                           ]
                                       result
                                       )
                                     )))
                    assoc-data (-> bp
                                   (assoc :main_categories main-cats*)
                                   (assoc :cacheKey (cache-key dashboard-cache-key bp))
                                   (assoc :total_price_cents (sum-total-price main-cats*)))
                    ]

                ;[clojure.data.json :as json]
                ;(println "\n>o> :assoc-data" assoc-data "\n")
                ;(println "\n>o> :assoc-data (json)" (json/write-str assoc-data) "\n")

                assoc-data
                )
              )
            )
       (printer "dashboard::determine-budget-periods/result")

       )
  )


(defn cast-ids-to-uuid [ids]
  (map #(java.util.UUID/fromString %) ids))


(defn get-dashboard
  [ctx args value]

  (println ">>>get-dashboard")
  (println ">>args" args)
  (println ">>value" value)

  (let [ring-request (:request ctx)
        tx (:tx-next ring-request)
        cat-ids (:category_id args)
        bp-ids (:budget_period_id args)


        p (println ">>mainCatsSql" (-> main-categories/main-categories-base-query
                                       sql-format))

        main-cats (-> main-categories/main-categories-base-query
                      sql-format
                      (->> (jdbc/execute! tx)))

        p (println ">>ToCheck mainCats" main-cats)

        bps (if (or (not bp-ids) (not-empty bp-ids))

              (let [
                    query (-> budget-periods/budget-periods-base-query
                              (cond-> bp-ids (sql/where [:in :procurement_budget_periods.id (cast-ids-to-uuid bp-ids)]))
                              sql-format)
                    p (println ">>queryA1" query)
                    result (jdbc/execute! tx query)

                    p (println ">>ToCheck dashboard:::procurement_budget_periods.id" result) ;; ids?

                    ] result)

              [])

        p (println ">>resultA1-2" bps)


        ;p (println ">>requestsB2-triggerError" ctx args value)
        p (println ">>requestsB2-triggerError" args value)

        requests (requests/get-requests ctx args value)     ;; FIXME: this causes issues, value=nil

        p (println ">o>requestsB2" requests)

        dashboard-cache-key {:id (hash args)}

        p (println ">o>dashboard-cache-keyB2" dashboard-cache-key)

        ]
    {:total_count (spy (count requests)),
     :cacheKey (spy (cache-key dashboard-cache-key)),
     :budget_periods (spy (determine-budget-periods requests tx dashboard-cache-key main-cats bps))
     }))
