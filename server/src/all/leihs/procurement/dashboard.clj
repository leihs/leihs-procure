(ns leihs.procurement.dashboard
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.resources.budget-periods :as budget-periods]
            [leihs.procurement.resources.categories :as categories]
            [leihs.procurement.resources.main-categories :as main-categories]
            [leihs.procurement.resources.requests :as requests]
            [leihs.procurement.utils.sql :as sql]))

(defn get-dashboard [ctx args value]
  (let [ring-request (:request ctx)
        tx (:tx ring-request)
        cat-ids (:category_id args)
        bp-ids (:budget_period_id args)
        cats (-> categories/categories-base-query
                 (cond-> cat-ids (sql/merge-where [:in :procurement_categories.id cat-ids]))
                 sql/format
                 (->> (jdbc/query tx)))
        main-cats (-> main-categories/main-categories-base-query
                      (cond-> cats (sql/merge-where
                                     [:in
                                      :procurement_main_categories.id
                                      (map :main_category_id cats)]))
                      sql/format
                      (->> (jdbc/query tx)))
        bps (-> budget-periods/budget-periods-base-query
                (cond-> bp-ids (sql/merge-where
                                 [:in :procurement_budget_periods.id bp-ids]))
                sql/format
                (->> (jdbc/query tx)))
        requests (requests/get-requests ctx args value)]
    (->> bps
         (map (fn [bp]
                (assoc bp
                       :main_categories
                       (->> main-cats
                            (map (fn [mc]
                                   (->> cats
                                        (filter #(= (:main_category_id %) (:id mc)))
                                        (map (fn [c]
                                               (->> requests
                                                    (filter #(and (= (-> % :category :value) (:id c))
                                                                  (= (-> % :budget_period :value) (:id bp))))
                                                    (assoc c :requests))))
                                        (assoc mc :categories)))))))))))
