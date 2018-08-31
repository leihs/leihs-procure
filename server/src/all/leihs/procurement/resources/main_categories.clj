(ns leihs.procurement.resources.main-categories
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [com.walmartlabs.lacinia [resolve :as resolve]]
            [leihs.procurement.graphql.helpers :refer
             [add-resource-type add-to-parent-values
              get-categories-args-from-context get-requests-args-from-context]]
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

(defn get-main-categories
  [context _ value]
  (let [tx (-> context
               :request
               :tx)
        requests-args (get-requests-args-from-context context)
        categories-args (get-categories-args-from-context context)
        main-categories (->> main-categories-base-query
                             sql/format
                             (jdbc/query tx)
                             (map #(-> %
                                       (add-resource-type :main-category)
                                       (add-to-parent-values value)
                                       (->> (merge-image-path tx)))))]
    (resolve/with-context main-categories
                          {:categories-args categories-args,
                           :requests-args requests-args})))

(defn get-main-categories-by-names
  [tx names]
  (jdbc/query tx
              (-> main-categories-base-query
                  (sql/where [:in :procurement_main_categories.name names])
                  (sql/order-by [:procurement_main_categories.name :asc])
                  sql/format)))

(defn delete-main-categories-not-in!
  [tx ids]
  (categories/delete-categories-not-in-main-category-ids! tx ids)
  (budget-limits/delete-budget-limits-not-in-main-category-ids! tx ids)
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_main_categories)
                     (sql/where [:not-in :procurement_main_categories.id ids])
                     sql/format)))

(defn update-main-categories!
  [context args _]
  (let [tx (-> context
               :request
               :tx)
        mcs (:input_data args)]
    (loop [[mc & rest-mcs] mcs
           mc-ids []]
      (if mc
        (let [mc-name (:name mc)]
          (do
            (if (:id mc)
              (main-category/update! tx (select-keys mc [:id :name]))
              (main-category/insert! tx {:name mc-name}))
            (let [mc-id (or (:id mc)
                            (->> mc-name
                                 (main-category/get-main-category-by-name tx)
                                 :id))
                  image (:image mc)
                  budget-limits (->> mc
                                     :budget_limits
                                     (map #(merge % {:main_category_id mc-id})))
                  categories (->> mc
                                  :categories
                                  (map #(merge % {:main_category_id mc-id})))]
              (if-not (empty? image)
                (main-category/deal-with-image! tx mc-id image))
              (budget-limits/update-budget-limits! tx budget-limits)
              (categories/update-categories! tx mc-id categories)
              (recur rest-mcs (conj mc-ids mc-id)))))
        (delete-main-categories-not-in! tx mc-ids)))
    (get-main-categories-by-names tx (map :name mcs))))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
