(ns leihs.procurement.resources.main-categories
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as logging]
            [leihs.procurement.paths :refer [path]]
            [leihs.procurement.resources.budget-limits :as budget-limits]
            [leihs.procurement.resources.image :as image]
            [leihs.procurement.resources.main-category :as main-category]
            [leihs.procurement.utils.sql :as sql]
            [logbug.debug :as debug]))

(def main-categories-base-query
  (-> (sql/select :procurement_main_categories.*)
      (sql/from :procurement_main_categories)))

(defn get-main-categories [context _ _]
  (let [tx (-> context :request :tx)]
    (->> main-categories-base-query
         sql/format
         (jdbc/query tx)
         (map (fn [mc]
                (let [image (->> (:id mc)
                                 image/image-query-for-main-category
                                 sql/format
                                 (jdbc/query tx)
                                 first)]
                  (if-let [image-id (debug/identity-with-logging (:id image))]
                    (merge mc {:image_url (path :image {:image-id image-id})})
                    ""))))
         )))

(defn get-main-categories-by-names [tx names]
  (jdbc/query tx
              (-> main-categories-base-query
                  (sql/where [:in :procurement_main_categories.name names])
                  (sql/order-by [:procurement_main_categories.name :asc])
                  sql/format)))

(defn update-main-categories! [context args _]
  (let [tx (-> context :request :tx)
        mcs (:input_data args)]
    (doseq [mc mcs]
      (let [mc-id (or (:id mc)
                      (let [mc-name (:name mc)]
                        (main-category/insert-main-category! tx {:name mc-name})
                        (->> mc-name (main-category/get-main-category-by-name tx) :id)))
            budget-limits (->> mc :budget_limits (map #(merge % {:main_category_id mc-id})))]
        (budget-limits/update-budget-limits! tx budget-limits)))
    (get-main-categories-by-names tx (map :name mcs))))

;#### debug ###################################################################
(logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
