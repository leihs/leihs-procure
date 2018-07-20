(ns leihs.procurement.resources.attachment
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
         (merge {:headers {"Content-Type" (:content_type a),
                           "Content-Transfer-Encoding" "binary"}}))
    {:status 404}))

(def attachment-path (path :attachment {:attachment-id ":attachment-id"}))

(def routes (cpj/routes (cpj/GET attachment-path [] #'attachment)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
