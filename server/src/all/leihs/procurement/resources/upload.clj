(ns leihs.procurement.resources.upload
  (:require [cheshire.core :rename {generate-string to-json}]
            [clojure.java.jdbc :as jdbc]
            [compojure.core :as cpj]
            [leihs.procurement.paths :refer [path]]
            [leihs.procurement.utils.exif :as exif]
            [leihs.procurement.utils.sql :as sql])
  (:import java.util.Base64
           org.apache.commons.io.FileUtils))

(defn insert-file-upload!
  [tx m]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_uploads)
                     (sql/values [m])
                     sql/format)))

(defn upload
  [{params :params, tx :tx}]
  (let [upload (:upload params)
        tempfile (:tempfile upload)
        content (->> tempfile
                     (FileUtils/readFileToByteArray)
                     (.encodeToString (Base64/getMimeEncoder)))
        metadata (-> tempfile
                     exif/extract-metadata
                     to-json
                     (#(sql/call :cast % :json)))
        upload-map (-> upload
                       (dissoc :tempfile)
                       (assoc :content content)
                       (assoc :metadata metadata))]
    (insert-file-upload! tx upload-map)
    (let [upload-row (-> (sql/select :id)
                         (sql/from :procurement_uploads)
                         (sql/order-by [:created_at :desc])
                         (sql/limit 1)
                         sql/format
                         (->> (jdbc/query tx))
                         first)]
      {:body upload-row})))

(def routes (cpj/routes (cpj/POST (path :upload) [] #'upload)))
