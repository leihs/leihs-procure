(ns leihs.procurement.resources.main-categories
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.procurement.paths :refer [path]]
   (leihs.procurement.resources [budget-limits :as budget-limits]
                                [categories :as categories] [image :as image]
                                [main-category :as main-category])
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]))

(def main-categories-base-query
  (-> (sql/select :procurement_main_categories.*)
      (sql/from :procurement_main_categories)
      (sql/order-by [:procurement_main_categories.name :asc])))

(defn merge-image-path
  [tx mc]
  (let [image (->> (:id mc)
                   image/image-query-for-main-category
                   sql-format
                   (jdbc/execute-one! tx))]
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
        sql-format
        (jdbc/execute! tx)
        (map #(transform-row tx %))))
  ([context _ _]
   (get-main-categories (-> context
                            :request
                            :tx))))

(defn get-main-categories-by-names
  [tx names]
  (jdbc/execute! tx
                 (-> main-categories-base-query
                     (sql/where [:in :procurement_main_categories.name names])
                     (sql/order-by [:procurement_main_categories.name :asc])
                     sql-format)))

(defn update-main-categories!
  [context args _]
  (let [tx (-> context
               :request
               :tx)
        mcs (:input_data args)]
    (loop [[mc & rest-mcs] mcs]
      (when mc
        (let [mc-id (atom (:id mc))]
          (if (:toDelete mc)
            (main-category/delete! tx @mc-id)
            (do
              (if @mc-id
                (main-category/update! tx (select-keys mc [:id :name]))
                (do (main-category/insert! tx (select-keys mc [:name]))
                    (reset! mc-id
                            (->> mc
                                 :name
                                 (main-category/get-main-category-by-name tx)
                                 :id))))
              (let [image (:new_image_url mc)
                    budget-limits
                    (->> mc
                         :budget_limits
                         (map #(merge % {:main_category_id @mc-id})))
                    categories (->> mc
                                    :categories
                                    (map #(merge %
                                                 {:main_category_id @mc-id})))]
                (if-not (empty? image)
                  (main-category/deal-with-image! tx @mc-id image))
                (budget-limits/update-budget-limits! tx budget-limits)
                (categories/update-categories! tx @mc-id categories)))))
        (recur rest-mcs)))
    (get-main-categories tx)))
