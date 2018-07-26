(ns leihs.procurement.resources.upload
  (:require [cheshire.core :refer [generate-string] :rename
             {generate-string to-json}]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
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
  [{params :params, tx :tx, :as request}]
  (let [upload (:upload params)
        tempfile (:tempfile upload)
        content (->> tempfile
                     (FileUtils/readFileToByteArray)
                     (.encodeToString (Base64/getMimeEncoder)))
        metadata (-> tempfile
                     exif/extract-metadata
                     to-json
                     (#(sql/call :cast % :json)))
        content-type (or (:content-type upload) (get metadata "File:MIMEType"))
        upload-map (-> upload
                       (dissoc :tempfile)
                       (assoc :content content)
                       (assoc :metadata metadata)
                       (assoc :content-type content-type))]
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
