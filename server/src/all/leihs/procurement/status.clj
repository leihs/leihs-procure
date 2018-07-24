(ns leihs.procurement.status
  (:require [leihs.procurement.paths :refer [path]]
            [compojure.core :as cpj]
            [clojure.tools.logging :as logging]
            [hiccup.page :refer [include-js html5]]
            [logbug.debug :as debug]))

(defn head
  []
  [:head [:meta {:charset "utf-8"}]
   [:meta
    {:name "viewport",
     :content "width=device-width, initial-scale=1, shrink-to-fit=no"}] ; (include-site-css)
   ; (include-font-css)
  ])

(defn status
  [request]
  {:headers {"Content-Type" "text/html"},
   :body (html5 (head)
                [:body
                 [:form {:action "/upload", :method "post"}
                  [:input {:type "file", :name "upload"}]
                  [:button {:type "submit"} "upload"]]])})

(def routes (cpj/routes (cpj/GET (path :status) [] #'status)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
