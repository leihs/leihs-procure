(ns leihs.procurement.dashboard
  (:require [clojure.string :as string]
            [clojure.data.json :as json]

            [taoensso.timbre :refer [debug info warn error spy]]

            [leihs.procurement.utils.helpers :refer [cast-uuids]]
            [clojure.data.json :as json]
    ;[clojure.java.jdbc :as jdbc]
    ;[leihs.procurement.utils.sql :as sql]

            [logbug.debug :as debug]


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
  (println ">o> sum-total-price ???" coll)
  (spy (->> (spy coll)
            (map :total_price_cents)
            (reduce +))))


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

(defn get-dashboard
  [ctx args value]

  (println ">debug> >>> FIRST LINE > get-dashboard????")

  (let [ring-request (:request ctx)
        tx (:tx-next ring-request)
        cat-ids (:category_id args)
        bp-ids (:budget_period_id args)
        main-cats (-> main-categories/main-categories-base-query
                      sql-format
                      (->> (jdbc/execute! tx)))


        ;; DEBUG
        main-cats-query (-> main-categories/main-categories-base-query
                            sql-format)
        p (println ">>> resultA1-2a xxx >query query-bps " main-cats-query)


        ;; DEBUG
        bps-query (-> budget-periods/budget-periods-base-query
                      (cond-> bp-ids (sql/where
                                       ;[:in :procurement_budget_periods.id (cast-ids-to-uuid bp-ids)]))
                                       [:in :procurement_budget_periods.id (cast-uuids bp-ids)]))
                      sql-format
                      )

        p (println ">>> resultA1-2b xxx >query query-bps " bps-query)



        bps (if (or (not bp-ids) (not-empty bp-ids))
              (-> budget-periods/budget-periods-base-query
                  (cond-> bp-ids (sql/where
                                   ;[:in :procurement_budget_periods.id (cast-ids-to-uuid bp-ids)]))
                                   [:in :procurement_budget_periods.id (cast-uuids bp-ids)]))
                  sql-format
                  (->> (jdbc/execute! tx)))
              [])

        p (println ">>resultA1-2 xxx bps" bps)


        requests (requests/get-requests ctx args value)
        p (println ">>requestsB2 xxx" requests)


        ;p (throw (Exception. "fake error"))

        dashboard-cache-key {:id (hash args)}]
    {:total_count (count requests),
     :cacheKey (cache-key dashboard-cache-key),
     :budget_periods (->>
                       bps
                       (map (fn [bp]
                              (let [main-cats* (->> main-cats
                                                    (map (fn [mc]
                                                           (let [cats* (->> mc
                                                                            :id
                                                                            (categories/get-for-main-category-id tx)
                                                                            (map (fn [c] (let [
                                                                                               p (println ">o> c ???1 xxx before filter  c=" c)
                                                                                               p (println ">o> c ???1 xxx before filter bp=" bp)

                                                                                               requests* (spy (filter #(and (= (-> %
                                                                                                                                   :category
                                                                                                                                   :value
                                                                                                                                   :id)
                                                                                                                               (str (:id c)))
                                                                                                                            (= (-> %
                                                                                                                                   :budget_period
                                                                                                                                   :value
                                                                                                                                   :id)
                                                                                                                               (str (:id bp))))
                                                                                                                      requests))
                                                                                               p (println ">>>id here ??? xxx requests*" requests*)
                                                                                               ]
                                                                                           (-> c
                                                                                               (assoc :requests requests*)
                                                                                               (assoc :total_price_cents (sum-total-price requests*))
                                                                                               (assoc :cacheKey
                                                                                                      (cache-key
                                                                                                        dashboard-cache-key
                                                                                                        bp
                                                                                                        mc
                                                                                                        c)))))))]
                                                             (-> mc
                                                                 (assoc :categories cats*)
                                                                 (assoc :total_price_cents (sum-total-price cats*))
                                                                 (assoc :cacheKey
                                                                        (cache-key dashboard-cache-key bp mc))
                                                                 (->> (main-categories/merge-image-path
                                                                        tx)))))))]
                                (-> bp
                                    (assoc :main_categories main-cats*)
                                    (assoc :cacheKey (cache-key dashboard-cache-key bp))
                                    (assoc :total_price_cents (sum-total-price main-cats*)))))))}))


;(debug/debug-ns *ns*)