(ns leihs.procurement.resources.image
  (:require [cheshire.core :refer [generate-string] :rename {generate-string to-json}]
            [compojure.core :as cpj]
            [honey.sql :refer [format] :rename {format sql-format}]
            [honey.sql.helpers :as sql]
            [leihs.procurement.paths :refer [path]]
            [leihs.procurement.resources.upload :as upload]
            [leihs.procurement.utils.helpers :refer [cast-to-json to-uuid]]
            [next.jdbc :as jdbc]
            [taoensso.timbre :refer [debug error info spy warn]])
  (:import java.util.Base64))

(def image-base-query
  (-> (sql/select :procurement_images.*)
      (sql/from :procurement_images)))

(defn image-query
  [id]
  (-> image-base-query
      (sql/where [:= :procurement_images.id (to-uuid id)])))

(defn image-query-for-main-category
  [id]
  (-> image-base-query
      (sql/where [:= :procurement_images.main_category_id id])))

(defn image
  [{tx :tx, {image-id :image-id} :route-params}]
  (if-let [i (->> image-id
                  image-query
                  sql-format
                  (jdbc/execute-one! tx))]
    (->> i
         :content
         (.decode (Base64/getMimeDecoder))
         (hash-map :body)
         (merge {:headers {"Content-Type" (:content_type i),
                           "Content-Transfer-Encoding" "binary"}}))
    {:status 404}))

(defn insert!
  [tx data]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_images)
                     (sql/values [data])
                     sql-format)))

(defn create-for-main-category-id-and-upload!
  [tx mc-id upload]
  (let [{u-id :id} upload
        u-row (upload/get-by-id tx u-id)
        md (-> u-row
               :metadata
               to-json
               cast-to-json)]
    (insert! tx
             (-> u-row
                 (dissoc :id)
                 (dissoc :created_at)
                 (assoc :metadata md)
                 (assoc :main_category_id mc-id)))
    (upload/delete! tx u-id)))

(def image-path (path :image {:image-id ":image-id"}))

(def routes (cpj/routes (cpj/GET image-path [] #'image)))
