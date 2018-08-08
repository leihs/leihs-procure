(ns leihs.procurement.resources.main-category
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.resources [image :as image] [images :as images]
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
                     sql/format)))

(defn- filter-images [m is] (filter #(submap? m %) is))

(defn deal-with-image!
  [tx mc-id images]
  (let [uploads-to-delete (filter-images {:to_delete true, :__typename "Upload"}
                                         images)
        uploads-to-images
          (filter-images {:to_delete false, :__typename "Upload"} images)
        images-to-delete (filter-images {:to_delete true, :__typename "Image"}
                                        images)
        ; NOTE: just for purpose of completeness and clarity:
        ; don't do anything with existing images
        ; images-to-retain
        ; (filter-images {:to_delete false, :__typename "Image"}
        ; images)
        ]
    (if-not (empty? uploads-to-delete)
      (uploads/delete! tx (map :id uploads-to-delete)))
    (if-not (empty? images-to-delete)
      (images/delete! tx (map :id images-to-delete)))
    (when-not (empty? uploads-to-images)
      (if (> (count uploads-to-images) 1)
        (throw (Exception. "Uploading of more than one image is not allowed.")))
      (image/create-for-main-category-id-and-upload! tx
                                                     mc-id
                                                     (first
                                                       uploads-to-images)))))

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

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
