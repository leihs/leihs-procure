(ns leihs.procurement.resources.upload
  (:require [cheshire.core :refer [generate-string] :rename
             {generate-string to-json}]
            [clojure.java.jdbc :as jdbc]
            [compojure.core :as cpj]
            [leihs.procurement.paths :refer [path]]
            [leihs.procurement.utils [exif :as exif] [sql :as sql]])
  (:import java.util.Base64
           org.apache.commons.io.FileUtils))

(defn insert-file-upload!
  [tx m]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_uploads)
                     (sql/values [m])
                     sql/format)))

(defn prepare-upload-row-map
  [file-data]
  (let [tempfile (:tempfile file-data)
        content (->> tempfile
                     (FileUtils/readFileToByteArray)
                     (.encodeToString (Base64/getMimeEncoder)))
        metadata (-> tempfile
                     exif/extract-metadata
                     to-json
                     (#(sql/call :cast % :json)))
        content-type (or (:content-type file-data)
                         (get metadata "File:MIMEType")
                         "application/octet-stream")]
    (-> file-data
        (dissoc :tempfile)
        (assoc :content content)
        (assoc :metadata metadata)
        (assoc :content-type content-type))))

(defn get-by-id
  [tx id]
  (-> (sql/select :procurement_uploads.*)
      (sql/from :procurement_uploads)
      (sql/merge-where [:= :procurement_uploads.id id])
      sql/format
      (->> (jdbc/query tx))
      first))

(defn upload
  [{params :params, tx :tx}]
  (let [files (:files params)
        files-data (if (vector? files) files [files])]
    (doseq [fd files-data]
      (->> fd
           prepare-upload-row-map
           (insert-file-upload! tx)))
    (let [upload-rows (-> (sql/select :id)
                          (sql/from :procurement_uploads)
                          (sql/order-by [:created_at :desc])
                          (sql/limit (count files-data))
                          sql/format
                          (->> (jdbc/query tx)))]
      {:body upload-rows})))

(def routes (cpj/routes (cpj/POST (path :upload) [] #'upload)))

(defn delete!
  [tx id]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_uploads)
                     (sql/where [:= :procurement_uploads.id id])
                     sql/format)))
