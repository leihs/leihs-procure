(ns leihs.procurement.resources.attachment
  (:require [clojure.java.jdbc :as jdbc]
            [compojure.core :as cpj]
            [leihs.procurement.paths :refer [path]]
            [leihs.procurement.utils.sql :as sql])
  (:import java.util.Base64))

(def attachment-base-query
  (-> (sql/select :procurement_attachments.*)
      (sql/from :procurement_attachments)))

(defn attachment-query
  [id]
  (-> attachment-base-query
      (sql/where [:= :procurement_attachments.id id])))

(defn attachment
  [{tx :tx, {attachment-id :attachment-id} :route-params}]
  (if-let [a (->> attachment-id
                  attachment-query
                  sql/format
                  (jdbc/query tx)
                  first)]
    (->> a
         :content
         (.decode (Base64/getMimeDecoder))
         (hash-map :body)
         (merge
           {:headers {"Content-Type" (:content_type a),
                      "Content-Transfer-Encoding" "binary",
                      "Content-Disposition"
                        (str "inline; " "filename=\"" (:filename a) "\"")}}))
    {:status 404}))

(def attachment-path (path :attachment {:attachment-id ":attachment-id"}))

(def routes (cpj/routes (cpj/GET attachment-path [] #'attachment)))

(defn create!
  [tx data]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_attachments)
                     (sql/values [data])
                     sql/format)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
