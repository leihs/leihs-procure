(ns leihs.procurement.resources.main-categories
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [com.walmartlabs.lacinia.resolve :as resolve]
            [leihs.procurement.paths :refer [path]]
            [leihs.procurement.resources [budget-limits :as budget-limits]
             [categories :as categories] [image :as image]
             [main-category :as main-category]]
            [leihs.procurement.utils.sql :as sql]))

(def main-categories-base-query
  (-> (sql/select :procurement_main_categories.*)
      (sql/from :procurement_main_categories)
      (sql/order-by [:procurement_main_categories.name :asc])))

(defn merge-image-path
  [tx mc]
  (let [image (->> (:id mc)
                   image/image-query-for-main-category
                   sql/format
                   (jdbc/query tx)
                   first)]
    (if-let [image-id (:id image)]
      (merge mc {:image_url (path :image {:image-id image-id})})
      mc)))

(defn transform-row
  [tx row]
  (as-> row <>
    (merge-image-path tx <>)
    (assoc <>
      :categories (->> <>
                       :id
                       (categories/get-for-main-category-id tx)))))

(defn get-main-categories
  ([tx]
   (->> main-categories-base-query
        sql/format
        (jdbc/query tx)
        (map #(transform-row tx %))))
  ([context _ _]
   (get-main-categories (-> context
                            :request
                            :tx))))

(defn get-main-categories-by-names
  [tx names]
  (jdbc/query tx
              (-> main-categories-base-query
                  (sql/where [:in :procurement_main_categories.name names])
                  (sql/order-by [:procurement_main_categories.name :asc])
                  sql/format)))

(defn update-main-categories!
  [context args _]
  (let [tx (-> context
               :request
               :tx)
        mcs (:input_data args)]
    (loop [[mc & rest-mcs] mcs]
      (when mc
        (with-local-vars [mc-id (:id mc)]
          (if (:toDelete mc)
            (main-category/delete! tx (var-get mc-id))
            (do
              (if (var-get mc-id)
                (main-category/update! tx (select-keys mc [:id :name]))
                (do (main-category/insert! tx (select-keys mc [:name]))
                    (var-set mc-id
                             (->> mc
                                  :name
                                  (main-category/get-main-category-by-name tx)
                                  :id))))
              (let [image (:image mc)
                    budget-limits
                      (->> mc
                           :budget_limits
                           (map #(merge % {:main_category_id (var-get mc-id)})))
                    categories (->> mc
                                    :categories
                                    (map #(merge %
                                                 {:main_category_id
                                                    (var-get mc-id)})))]
                (if-not (empty? image)
                  (main-category/deal-with-image! tx (var-get mc-id) image))
                (budget-limits/update-budget-limits! tx budget-limits)
                (categories/update-categories! tx
                                               (var-get mc-id)
                                               categories)))))
        (recur rest-mcs)))
    (get-main-categories tx)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
