(ns leihs.procurement.resources.upload
  (:require [cheshire.core :refer [generate-string] :rename
             {generate-string to-json}]
            [clojure.string :as string]
            [compojure.core :as cpj]
            [honey.sql :refer [format] :rename {format sql-format}]
            [honey.sql.helpers :as sql]
            [leihs.procurement.paths :refer [path]]
            (leihs.procurement.utils [exif :as exif])
            [leihs.procurement.utils.helpers :refer [cast-to-json]]
            [next.jdbc :as jdbc]
            [taoensso.timbre :refer [debug error info spy warn]])

  (:import java.util.Base64
           org.apache.commons.io.FileUtils))

(defn insert-file-upload!
  [tx m]
  (let [result (jdbc/execute-one! tx (-> (sql/insert-into :procurement_uploads)
                                         (sql/values [m])
                                         sql-format))]
    (:update-count result)))

(defn prepare-upload-row-map
  [file-data]
  (let [tempfile (:tempfile file-data)
        content (->> tempfile
                     (FileUtils/readFileToByteArray)
                     (.encodeToString (Base64/getMimeEncoder)))
        metadata (-> tempfile
                     exif/extract-metadata
                     cast-to-json)
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
  (-> (sql/select :procurement_uploads.*)
      (sql/from :procurement_uploads)
      (sql/where [:= :procurement_uploads.id id])
      sql-format
      (->> (jdbc/execute-one! tx))))

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
                          sql-format
                          (->> (jdbc/execute! tx)))]
      {:body upload-rows})))

(def routes (cpj/routes (cpj/POST (path :upload) [] #'upload)))

(defn delete!
  [tx id]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_uploads)
                     (sql/where [:= :procurement_uploads.id id])
                     sql-format)))
