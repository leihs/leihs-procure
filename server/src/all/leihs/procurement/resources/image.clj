(ns leihs.procurement.resources.image
  (:require [cheshire.core :refer [generate-string] :rename
             {generate-string to-json}]
            [clojure.java.jdbc :as jdbc]
            [compojure.core :as cpj]
            [leihs.procurement.paths :refer [path]]
            [leihs.procurement.resources.upload :as upload]
            [leihs.procurement.utils.sql :as sql])
  (:import java.util.Base64))

(def image-base-query
  (-> (sql/select :procurement_images.*)
      (sql/from :procurement_images)))

(defn image-query
  [id]
  (-> image-base-query
      (sql/where [:= :procurement_images.id id])))

(defn image-query-for-main-category
  [id]
  (-> image-base-query
      (sql/where [:= :procurement_images.main_category_id id])))

(defn image
  [{tx :tx, {image-id :image-id} :route-params}]
  (if-let [i (->> image-id
                  image-query
                  sql/format
                  (jdbc/query tx)
                  first)]
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
                     sql/format)))

(defn create-for-main-category-id-and-upload!
  [tx mc-id upload]
  (let [{u-id :id} upload
        u-row (upload/get-by-id tx u-id)
        md (-> u-row
               :metadata
               to-json
               (#(sql/call :cast % :json)))]
    (insert! tx
             (-> u-row
                 (dissoc :id)
                 (dissoc :created_at)
                 (assoc :metadata md)
                 (assoc :main_category_id mc-id)))
    (upload/delete! tx u-id)))

(def image-path (path :image {:image-id ":image-id"}))

(def routes (cpj/routes (cpj/GET image-path [] #'image)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
