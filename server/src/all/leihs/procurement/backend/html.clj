(ns leihs.procurement.backend.html
  (:refer-clojure :exclude [str keyword])
  (:require [hiccup.page :refer [html5]]))

(defn head
  []
  [:head [:meta {:charset "utf-8"}]
   [:meta
    {:name "viewport",
     :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]])

(defn not-found-handler
  [request]
  {:status 404,
   :headers {"Content-Type" "text/html"},
   :body (html5 (head)
                [:body
                 [:div.container-fluid
                  [:h1.text-danger "Error 404 - Not Found"]]])})

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns 'cider-ci.utils.shutdown)
;(debug/debug-ns *ns*)
