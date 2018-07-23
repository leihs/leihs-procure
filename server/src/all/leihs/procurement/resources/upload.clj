(ns leihs.procurement.resources.upload
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [compojure.core :as cpj]
            [leihs.procurement.paths :refer [path]]
            [leihs.procurement.utils.sql :as sql])
  (:import [java.util Base64]
           [org.apache.commons.io FileUtils]))

(defn insert-file-upload!
  [tx m]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_uploads)
                     (sql/values [m])
                     sql/format)))

(defn upload
  [{params :params, tx :tx}]
  (let [upload (:upload params)
        content (->> upload
                     :tempfile
                     (FileUtils/readFileToByteArray)
                     (.encodeToString (Base64/getMimeEncoder)))
        upload-map (-> upload
                       (dissoc :tempfile)
                       (assoc :content content))]
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
