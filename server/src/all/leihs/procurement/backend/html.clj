(ns leihs.procurement.backend.html
  (:require [ring.util.response :refer
             [resource-response
              content-type
              status]]))

(defn index-html-response
  []
  (-> "public/procure/client/index.html"
      resource-response
      (content-type "text/html")))

(defn not-found-handler
  [_]
  (-> (index-html-response)
      (status 404)))

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns 'cider-ci.utils.shutdown)
;(debug/debug-ns *ns*)
