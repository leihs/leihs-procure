(ns leihs.procurement.resources.image
  (:require [cheshire.core :refer [generate-string] :rename
             {generate-string to-json}]
            
    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]

                [taoensso.timbre :refer [debug info warn error spy]]



    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]
    
            [compojure.core :as cpj]
            [leihs.procurement.paths :refer [path]]
            [leihs.procurement.resources.upload :as upload]
    )
  (:import java.util.Base64))

(def image-base-query
  (-> (sql/select :procurement_images.*)
      (sql/from :procurement_images)))

(defn image-query
  [id]
  (-> image-base-query
      (sql/where [:= :procurement_images.id [:cast id :uuid]])))

(defn image-query-for-main-category
  [id]
  (-> image-base-query
      (sql/where [:= :procurement_images.main_category_id [:cast id :uuid]])))

(defn image
  [{tx :tx-next, {image-id :image-id} :route-params}]
  (if-let [i (->> image-id
                  image-query
                  sql-format
                  (jdbc/execute-one! tx)
                  )]
    (->> i
         :content
         (.decode (Base64/getMimeDecoder))
         (hash-map :body)
         (merge {:headers {"Content-Type" (:content_type i),
                           "Content-Transfer-Encoding" "binary"}}))
    {:status 404}))

(defn cast-to-json [comment] [:cast comment :json])

(defn insert!
  [tx data]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_images)
                     (sql/values [data])
                     sql-format)))

(defn create-for-main-category-id-and-upload!
  [tx mc-id upload]
  (let [{u-id :id} upload
        p (println ">o> u-id" u-id)

        u-row (upload/get-by-id tx (spy u-id))
        p (println ">o> u-row" u-row)

        md (-> u-row
               :metadata
               to-json
               cast-to-json

               ;(#( :cast % :json))
               )]
    (insert! tx
             (-> u-row
                 (dissoc :id)
                 (dissoc :created_at)
                 (assoc :metadata md)
                 (assoc :main_category_id mc-id)))
    (upload/delete! tx u-id)))

(def image-path (path :image {:image-id ":image-id"}))

(def routes (cpj/routes (cpj/GET image-path [] #'image)))
