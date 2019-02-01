(ns leihs.procurement.resources.main-category
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.resources [budget-limits :as budget-limits]
             [categories :as categories] [image :as image] [images :as images]
             [uploads :as uploads]]
            [leihs.procurement.utils [helpers :refer [submap?]] [sql :as sql]]))

(def main-category-base-query
  (-> (sql/select :procurement_main_categories.*)
      (sql/from :procurement_main_categories)))

(defn main-category-query-by-id
  [id]
  (-> main-category-base-query
      (sql/where [:= :procurement_main_categories.id id])
      sql/format))

(defn main-category-query-by-name
  [mc-name]
  (-> main-category-base-query
      (sql/where [:= :procurement_main_categories.name mc-name])
      sql/format))

(defn get-main-category
  [context _ value]
  (-> value
      :main_category_id
      main-category-query-by-id
      (->> (jdbc/query (-> context
                           :request
                           :tx)))
      first))

(defn get-main-category-by-name
  [tx mc-name]
  (first (jdbc/query tx (main-category-query-by-name mc-name))))

(defn insert!
  [tx mc]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_main_categories)
                     (sql/values [mc])
                     sql/format
                     log/spy)))

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
                       sql/format))
    (image/create-for-main-category-id-and-upload! tx mc-id new-image-upload))
  (when-let [uploads-to-delete (-> {:to_delete true, :typename "Upload"}
                                   (filter-images images)
                                   not-empty)]
    (uploads/delete! tx (map :id uploads-to-delete))))

(defn update!
  [tx mc]
  (jdbc/execute! tx
                 (-> (sql/update :procurement_main_categories)
                     (sql/sset mc)
                     (sql/where [:= :procurement_main_categories.id (:id mc)])
                     sql/format)))

(defn can-delete?
  [context _ value]
  (->
    (jdbc/query
      (-> context
          :request
          :tx)
      (->
        (sql/call
          :and
          (sql/call :not
                    (sql/call :exists
                              (-> (sql/select true)
                                  (sql/from [:procurement_requests :pr])
                                  (sql/merge-join [:procurement_categories :pc]
                                                  [:= :pc.id :pr.category_id])
                                  (sql/merge-where [:= :pc.main_category_id
                                                    (:id value)]))))
          (sql/call :not
                    (sql/call :exists
                              (-> (sql/select true)
                                  (sql/from [:procurement_templates :pt])
                                  (sql/merge-join [:procurement_categories :pc]
                                                  [:= :pc.id :pt.category_id])
                                  (sql/merge-where [:= :pc.main_category_id
                                                    (:id value)])))))
        (vector :result)
        sql/select
        sql/format))
    first
    :result))

(defn delete!
  [tx id]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_main_categories)
                     (sql/where [:= :procurement_main_categories.id id])
                     sql/format)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
