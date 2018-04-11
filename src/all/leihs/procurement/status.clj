(ns leihs.procurement.status
  (:require
    [leihs.procurement.paths :refer [path]]
    [compojure.core :as cpj]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]))

(defn status [request]
  {:body {}})

(def routes
  (cpj/routes
    (cpj/GET (path :status) [] #'status)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
