(ns leihs.procurement.resources.image
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.data.codec.base64 :as base64]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.io :as io]
            [clojure.tools.logging :as logging]
            [compojure.core :as cpj]
            [leihs.procurement.paths :refer [path]]
            [leihs.procurement.utils.ds :as ds]
            [leihs.procurement.utils.sql :as sql]
            [logbug.debug :as debug])
  (:import [java.util Base64]))

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
  (let [i (->> image-id
               image-query
               sql/format
               (jdbc/query tx)
               first)]
    (->> i
         :content
         (.decode (Base64/getMimeDecoder))
         (hash-map :body)
         (merge {:headers {"Content-Type" (:content_type i),
                           "Content-Disposition"
                             (str "inline; filename=\"" (:filename i) "\""),
                           "Content-Transfer-Encoding" "binary"}}))))

(def image-path (path :image {:image-id ":image-id"}))

(def routes (cpj/routes (cpj/GET image-path [] #'image)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
