(ns leihs.procurement.resources.upload
  (:require [clojure.string :as string]
            [cheshire.core :refer [generate-string] :rename
             {generate-string to-json}]
            [taoensso.timbre :refer [debug info warn error spy]]

    ;[clojure.java.jdbc :as jdbc]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]
    
            [compojure.core :as cpj]
            [leihs.procurement.paths :refer [path]]
            [leihs.procurement.utils [exif :as exif] 
             ;[sql :as sql]
             ])
  (:import java.util.Base64
           org.apache.commons.io.FileUtils))

(defn insert-file-upload!
  [tx m]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_uploads)
                     (sql/values [m])
                     sql-format
                     spy
                     )))

(defn prepare-upload-row-map
  [file-data]
  (let [tempfile (:tempfile file-data)
        content (->> tempfile
                     (FileUtils/readFileToByteArray)
                     (.encodeToString (Base64/getMimeEncoder)))
        metadata (-> tempfile
                     exif/extract-metadata
                     to-json
                     (#( :cast % :json)))
        content-type (or (:content-type file-data)
                         (get metadata "File:MIMEType")
                         "application/octet-stream")]
    (-> file-data
        (dissoc :tempfile)
        (assoc :content content)
        (assoc :metadata metadata)
        (assoc :content-type content-type)
        (assoc :exiftool_version (exif/exiftool-version))
        (assoc :exiftool_options (string/join " " exif/exiftool-options)))))

(defn get-by-id
  [tx id]
  (spy (-> (sql/select :procurement_uploads.*)
      (sql/from :procurement_uploads)
      (sql/where [:= :procurement_uploads.id [:cast id :uuid]])
      sql-format
      spy
      (->> (jdbc/execute-one! tx))
      )))

(defn upload
  [{params :params, tx :tx-next}]
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
                          sql-format
                          spy
                          (->> (jdbc/execute! tx)))]
      {:body (spy upload-rows)})))

(def routes (cpj/routes (cpj/POST (path :upload) [] #'upload)))

(defn delete!
  [tx id]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_uploads)
                     (sql/where [:= :procurement_uploads.id [:cast id :uuid]])
                     sql-format)))
