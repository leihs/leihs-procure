(ns leihs.procurement.dashboard
  (:require [clojure.string :as string]
    

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
            [leihs.procurement.resources.requests :as requests]
    ))

(defn sum-total-price
  [coll]
  (->> coll
       (map :total_price_cents)
       (reduce +)))

(defn cache-key
  [& args]
  (->> args
       (map :id)
       (string/join "_")))

(defn get-dashboard
  [ctx args value]

  (println ">>>get-dashboard")
  (println ">>args" args)
  (println ">>value" value)
  ;(spy value)

  (let [ring-request (:request ctx)
        tx (:tx-next ring-request)
        cat-ids (:category_id args)
        bp-ids (:budget_period_id args)


        p (println ">>mainCatsSql" (-> main-categories/main-categories-base-query
                                    sql-format))

        main-cats (-> main-categories/main-categories-base-query
                      sql-format
                      (->> (jdbc/execute! tx)))

        p (println ">>mainCats" main-cats)

        bps (if (or (not bp-ids) (not-empty bp-ids))

              ;(-> budget-periods/budget-periods-base-query
              ;    (cond-> bp-ids (sql/where
              ;                     [:in :procurement_budget_periods.id bp-ids]))
              ;    sql-format
              ;    (->> (jdbc/execute! tx)))


              (let [
                    query (-> budget-periods/budget-periods-base-query
                              (cond-> bp-ids (sql/where
                                               [:in :procurement_budget_periods.id bp-ids]))
                              sql-format)
                    p (println ">>queryA1" query)
                    result (jdbc/execute! tx query)

                    p (println ">>resultA1" result)

                    ]result)


              [])
        ;p (println ">>requestsB2-triggerError" ctx args value)
        p (println ">>requestsB2-triggerError" args value)

        requests (requests/get-requests ctx args value)     ;; FIXME: this causes issues, value=nil

        p (println ">>requestsB2" requests)

        dashboard-cache-key {:id (hash args)}

        p (println ">>dashboard-cache-keyB2" dashboard-cache-key)


        ]
    {:total_count (count requests),
     :cacheKey (cache-key dashboard-cache-key),
     :budget_periods
       (->>
         bps
         (map
           (fn [bp]
             (let [main-cats*
                     (->>
                       main-cats
                       (map
                         (fn [mc]
                           (let [cats*
                                   (->>
                                     mc
                                     :id
                                     (categories/get-for-main-category-id tx)
                                     (map
                                       (fn [c]
                                         (let [requests*
                                                 (filter
                                                   #(and (= (-> %
                                                                :category
                                                                :value
                                                                :id)
                                                            (str (:id c)))
                                                         (= (-> %
                                                                :budget_period
                                                                :value
                                                                :id)
                                                            (str (:id bp))))
                                                   requests)]
                                           (-> c
                                               (assoc :requests requests*)
                                               (assoc :total_price_cents
                                                        (sum-total-price
                                                          requests*))
                                               (assoc :cacheKey
                                                        (cache-key
                                                          dashboard-cache-key
                                                          bp
                                                          mc
                                                          c)))))))]
                             (-> mc
                                 (assoc :categories cats*)
                                 (assoc :total_price_cents (sum-total-price
                                                             cats*))
                                 (assoc :cacheKey
                                          (cache-key dashboard-cache-key bp mc))
                                 (->> (main-categories/merge-image-path
                                        tx)))))))]
               (-> bp
                   (assoc :main_categories main-cats*)
                   (assoc :cacheKey (cache-key dashboard-cache-key bp))
                   (assoc :total_price_cents (sum-total-price
                                               main-cats*)))))))}))
