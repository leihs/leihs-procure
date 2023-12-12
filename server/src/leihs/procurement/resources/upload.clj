(ns leihs.procurement.resources.upload
  (:require [cheshire.core :refer [generate-string] :rename
             {generate-string to-json}]
            [cheshire.core :as json]
            [clojure.string :as string]

            [compojure.core :as cpj]

    ;[clojure.java.jdbc :as jdbc]

            [honey.sql :refer [format] :rename {format sql-format}]
            [honey.sql.helpers :as sql]
            [leihs.core.db :as db]
            [leihs.procurement.paths :refer [path]]

            (leihs.procurement.utils [exif :as exif]
                                     ;[sql :as sql]
                                     )
            [next.jdbc :as jdbc]
            [taoensso.timbre :refer [debug error info spy warn]])
  (:import java.util.Base64
           org.apache.commons.io.FileUtils))






(defn my-cast [data]
  (println ">o> no / 22 / my-cast /debug " data)


  (let [
        data (if (contains? data :id)
               (assoc data :id [[:cast (:id data) :uuid]])
               data
               )

        data (if (contains? data :category_id)
               (assoc data :category_id [[:cast (:category_id data) :uuid]])
               data
               )
        data (if (contains? data :template_id)
               (assoc data :template_id [[:cast (:template_id data) :uuid]])
               data
               )

        data (if (contains? data :room_id)
               (assoc data :room_id [[:cast (:room_id data) :uuid]])
               data
               )

        data (if (contains? data :order_status)
               (assoc data :order_status [[:cast (:order_status data) :order_status_enum]])
               data
               )

        data (if (contains? data :budget_period_id)
               (assoc data :budget_period_id [[:cast (:budget_period_id data) :uuid]])
               data
               )

        data (if (contains? data :user_id)
               (assoc data :user_id [[:cast (:user_id data) :uuid]])
               data
               )

        data (if (contains? data :metadata)
               (do
                 (println ">o> upload::metadata")
                 (assoc data :metadata [[:cast (:metadata data) :jsonb]]) ;; works as local-test
                 ;(assoc data :metadata [[:cast (:metadata data) :json]])
                 ;(assoc data :metadata [[:cast (:metadata data) :text]]))
                 )
               data
               )

        ;[[:cast (to-name-and-lower-case a) :order_status_enum]]

        ]
    (spy data)
    )

  )



(defn insert-file-upload!
  [tx m]

  ;{:filename "thx.png", :content-type "image/png", :size 1669, :content "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\r\n7JSO3AAAAABJRU5ErkJggg==",
  ; :metadata :json, :exiftool_version "12.70", :exiftool_options "-j -s -a -u -G1"}


  (let [
        m (my-cast m)

        p (println ">o> m" m)
        p (println ">o> :metadata" (:metadata m) "\n")

        result (jdbc/execute-one! tx
                              (spy (-> (sql/insert-into :procurement_uploads)
                                       (sql/values [(spy m)])
                                       sql-format
                                       spy
                                       )))
        ] (spy (:update-count result)))

  )


(comment

  (let [
        tx (db/get-ds-next)
        request {:route-params {:user-id #uuid "c0777d74-668b-5e01-abb5-f8277baa0ea8"}
                 :tx tx}
        user-id #uuid "37bb3d3d-3a61-4f98-863e-c549568317f0"

        json_data (json/generate-string {:ab "test" :cd "efg"})

        ;; procure-logic
        json_data (to-json json_data)



        data (my-cast {:size 12 :filename "test.me" :metadata json_data :content "abcdef"})

        result (jdbc/execute-one! tx (spy (-> (sql/insert-into :procurement_uploads)
                                              (sql/values [data])
                                              sql-format
                                              spy
                                              )))

        p (println "\nresult1" result)
        p (println "\nresult2" (:update-count result))      ;; nil
        p (println "\nresult3" (:next.jdbc/update-count result)) ;; 1

        ]

    )
  )

(defn prepare-upload-row-map
  [file-data]
  (let [tempfile (:tempfile file-data)
        content (->> tempfile
                     (FileUtils/readFileToByteArray)
                     (.encodeToString (Base64/getMimeEncoder)))
        metadata (-> tempfile
                     exif/extract-metadata
                     to-json
                     ;(#(:cast % :json))
                     )
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
