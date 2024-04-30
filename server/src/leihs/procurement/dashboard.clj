(ns leihs.procurement.dashboard
  (:require [clojure.string :as string]
            [honey.sql :refer [format] :rename {format sql-format}]
            [honey.sql.helpers :as sql]
            [leihs.procurement.resources.budget-periods :as budget-periods]
            [leihs.procurement.resources.categories :as categories]
            [leihs.procurement.resources.main-categories :as main-categories]
            [leihs.procurement.resources.requests :as requests]
            [leihs.procurement.utils.helpers :refer [convert-dates]]
            [next.jdbc :as jdbc]
            [taoensso.timbre :refer [debug error info spy warn]]))

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

(defn generate_categories [mc requests bp dashboard-cache-key tx]
  (->> mc
       :id
       (categories/get-for-main-category-id tx)
       (map (fn [c] (let [requests* (->> requests
                                         (filter #(and (= (-> % :category :value :id) (str (:id c)))
                                                       (= (-> % :budget_period :value :id) (str (:id bp))))))]
                      (-> c
                          (assoc :requests requests*)
                          (assoc :total_price_cents (sum-total-price requests*))
                          (assoc :cacheKey
                                 (cache-key
                                  dashboard-cache-key
                                  bp
                                  mc
                                  c))))))))

(defn create-budget [bps tx requests dashboard-cache-key main-cats]
  (->>
   bps
   (map (fn [bp]
          (let [main-cats* (->> main-cats
                                (map (fn [mc]
                                       (let [cats* (generate_categories mc requests bp dashboard-cache-key tx)
                                             merged-path (-> mc
                                                             (assoc :categories cats*)
                                                             (assoc :total_price_cents (sum-total-price cats*))
                                                             (assoc :cacheKey
                                                                    (cache-key dashboard-cache-key bp mc))
                                                             (->> (main-categories/merge-image-path
                                                                   tx)))]
                                         merged-path))))]
            (-> bp
                (assoc :main_categories main-cats*)
                (assoc :cacheKey (cache-key dashboard-cache-key bp))
                (assoc :total_price_cents (sum-total-price main-cats*))))))))

(defn cache-key
  [& args]
  (->> args
       (map :id)
       (string/join "_")))

(defn get-dashboard
  [ctx args value]
  (let [ring-request (:request ctx)
        tx (:tx ring-request)
        cat-ids (:category_id args)
        bp-ids (:budget_period_id args)
        main-cats (-> main-categories/main-categories-base-query
                      sql-format
                      (->> (jdbc/execute! tx)))
        bps (if (or (not bp-ids) (not-empty bp-ids))
              (-> budget-periods/budget-periods-base-query
                  (cond-> bp-ids (sql/where [:in :procurement_budget_periods.id bp-ids]))
                  sql-format
                  (->> (jdbc/execute! tx)))
              [])
        bps (map convert-dates bps)
        requests (requests/get-requests ctx args value)
        dashboard-cache-key {:id (hash args)}]
    {:total_count (count requests),
     :cacheKey (cache-key dashboard-cache-key),
     :budget_periods (create-budget bps tx requests dashboard-cache-key main-cats)}))
