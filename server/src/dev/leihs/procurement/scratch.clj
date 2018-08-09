; just for testing purposes
(ns leihs.procurement.scratch
  (:require [compojure.core :as cpj]
            [hiccup.page :refer [html5]]
            [leihs.procurement.backend.html :refer [head]]
            [leihs.procurement.paths :refer [path]]))

(defn scratch
  [request]
  {:headers {"Content-Type" "text/html"},
   :body
     (html5
       (head)
       [:body
        [:div#app.container-fluid
         [:div.alert.alert-warning [:h1 "Scratch Page"]
          [:h2 "(just for local test purposes)"]
          [:img {:src "/procure/images/b33f4c7d-409e-4486-8484-a91b766d436d"}]
          [:img
           {:src "/procure/images/cef1bb75-d75d-46af-a47e-2d03876b2a0b"}]]]])})

(def routes (cpj/routes (cpj/GET (path :scratch) [] #'scratch)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
