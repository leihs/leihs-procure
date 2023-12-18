(ns leihs.procurement.dashboard
  (:require [clojure.string :as string]
            [clojure.data.json :as json]

            [taoensso.timbre :refer [debug info warn error spy]]

            [leihs.procurement.utils.helpers :refer [cast-uuids]]
            [clojure.data.json :as json]
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

(defn sum-total-price
  [coll]
  (->> coll
       (map :total_price_cents)
       (reduce +)))


;(comment
;
;  (let [
;        ss [{:total_price_cents 1000}
;            {:total_price_cents 2000}
;            {:total_price_cents nil}
;            {:total_price_cents 1500}]
;
;        ;ss [{}]
;
;        p (println ">oo>" (sum-total-price ss))
;
;        ])
;  )


(defn cache-key
  [& args]
  (->> args
       (map :id)
       (string/join "_")))

(defn my-log [req]

  ;[clojure.data.json :as json]
    (println ">>>id1 (json)" (json/write-str req))
  ;  ;(println ">>>id2 " req)
  ;  (println ">>>id2 " (get-in (first req) [:category :read]))
  ;  ;(println ">>>id2a " (get-in (first req) [:budget_period :request_id]))
  ;  (println ">>>id2b " (get-in (first req) [:budget_period]))
  ;  (println ">>>id2c " (get-in (first req) [:budget_period :request-id])) ;; correct
  ;  (println ">>>id2c " (get-in (first req) [:category :request-id])) ;; correct
  ;  (println ">>>id3 " (first req))
  ;  ;(println ">>>id4 " :budget_period req)
  ;  (println ">>>id5 " :budget_period (first req))
  ;  (println ">>>id6 " (:request_id (:budget_period (first req))))

  (let [
        budget (get-in (first req) [:budget_period :request-id]) ;; correct
        category (get-in (first req) [:category :value :id]) ;; correct
        main-cat (get-in (first req) [:category :value :main_category_id]) ;; correct

        p (println ">>>id   main-cat=" main-cat " cat=" category " budget=" budget)
        ])


  req

  )

(defn get-dashboard
  [ctx args value]
  (let [ring-request (:request ctx)
        tx (:tx-next ring-request)
        cat-ids (:category_id args)
        bp-ids (:budget_period_id args)
        main-cats (-> main-categories/main-categories-base-query
                      sql-format
                      (->> (jdbc/execute! tx)))
        bps (if (or (not bp-ids) (not-empty bp-ids))
              (-> budget-periods/budget-periods-base-query
                  (cond-> bp-ids (sql/where
                                   ;[:in :procurement_budget_periods.id (cast-ids-to-uuid bp-ids)]))
                                   [:in :procurement_budget_periods.id (cast-uuids bp-ids)]))
                  sql-format
                  (->> (jdbc/execute! tx)))
              [])
        requests (requests/get-requests ctx args value)
        dashboard-cache-key {:id (hash args)}]
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
                               (map (fn [c] (let [requests* (filter #(and (= (-> %
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
                                                  p (println ">>>id requests*" requests*)
                                                  ]
                                              (-> c
                                                  (assoc :requests (my-log requests*))
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
