(ns leihs.procurement.shutdown
  (:require
    [leihs.procurement.paths :refer [path]]
    [compojure.core :as cpj]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]))

(defn- shutdown [request]
  (do (future (Thread/sleep 250)
              (System/exit 0))
      {:status 204}))

(def routes
  (cpj/routes
    (cpj/POST (path :shutdown) [] #'shutdown)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
