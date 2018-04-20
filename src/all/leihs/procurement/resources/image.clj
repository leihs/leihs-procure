(ns leihs.procurement.resources.image
  (:require
    [clj-logging-config.log4j :as logging-config]
    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [leihs.procurement.paths :refer [path]]
    [leihs.procurement.utils.sql :as sql]
    [logbug.debug :as debug]
    ))

(defn image-query [id]
  (-> (sql/select :procurement_images.*)
      (sql/from [:procurement_images :pi])
      (sql/where [:= :pi.id id])
      sql/format))

(defn image [{tx :tx {image-id :image-id} :route-params}]
  {:body
   (first (jdbc/query tx (image-query image-id)))})

(def image-path (path :image {:image-id ":image-id"}))

(def routes
  (cpj/routes
    (cpj/GET image-path [] #'image)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
