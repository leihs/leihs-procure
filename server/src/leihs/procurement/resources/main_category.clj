(ns leihs.procurement.resources.main-category
  (:require
    [clojure.tools.logging :as log]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    (leihs.procurement.resources [image :as image] [uploads :as uploads])
    (leihs.procurement.utils [helpers :refer [submap?]])
    [next.jdbc :as jdbc]
    [taoensso.timbre :refer [debug error info spy warn]]))

(def main-category-base-query
  (-> (sql/select :procurement_main_categories.*)
      (sql/from :procurement_main_categories)))

(defn main-category-query-by-id
  [id]
  (-> main-category-base-query
      (sql/where [:= :procurement_main_categories.id id])
      sql-format))

(defn main-category-query-by-name
  [mc-name]
  (-> main-category-base-query
      (sql/where [:= :procurement_main_categories.name mc-name])
      sql-format))

(defn get-main-category
  [context _ value]
  (-> value
      :main_category_id
      main-category-query-by-id
      (->> (jdbc/execute! (-> context
                           :request
                           :tx-next)))
      first))

(defn get-main-category-by-name
  [tx mc-name]
  (first (jdbc/execute! tx (main-category-query-by-name mc-name))))

(defn insert!
  [tx mc]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_main_categories)
                     (sql/values [mc])
                     sql-format)))

(defn- filter-images [m is] (filter #(submap? m %) is))

(defn deal-with-image!
  [tx mc-id images]
  (log/info images)
  (when-let [new-image-upload (-> {:to_delete false, :typename "Upload"}
                                  (filter-images images)
                                  first)]
    (jdbc/execute! tx
                   (-> (sql/delete-from :procurement_images)
                       (sql/where [:= :procurement_images.main_category_id
                                   mc-id])
                       sql-format))
    (image/create-for-main-category-id-and-upload! tx mc-id new-image-upload))
  (when-let [uploads-to-delete (-> {:to_delete true, :typename "Upload"}
                                   (filter-images images)
                                   not-empty)]
    (uploads/delete! tx (map :id uploads-to-delete))))

(defn update!
  [tx mc]
  (jdbc/execute! tx
                 (-> (sql/update :procurement_main_categories)
                     (sql/set mc)
                     (sql/where [:= :procurement_main_categories.id (:id mc)])
                     sql-format)))

(defn can-delete?
  [context _ value]
  (->
    (jdbc/execute-one!
      (-> context
          :request
          :tx-next)
      (-> [:and
           [:not
            [:exists
             (-> (sql/select true)
                 (sql/from [:procurement_requests :pr])
                 (sql/join [:procurement_categories :pc]
                           [:= :pc.id :pr.category_id])
                 (sql/where [:= :pc.main_category_id
                             (:id value)]))]]
           [:not
            [:exists
             (-> (sql/select true)
                 (sql/from [:procurement_templates :pt])
                 (sql/join [:procurement_categories :pc]
                           [:= :pc.id :pt.category_id])
                 (sql/where [:= :pc.main_category_id
                             (:id value)]))]]]
        (vector :result)
        sql/select
        sql-format))
    :result))

(defn delete!
  [tx id]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_main_categories)
                     (sql/where [:= :procurement_main_categories.id id])
                     sql-format)))
