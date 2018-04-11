(ns leihs.procurement.backend.html
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.procurement.utils.core :refer [keyword str presence]])
  (:require
    [leihs.procurement.env :refer [env]]
    [leihs.procurement.resources.user :as user]
    [leihs.procurement.utils.http-resources-cache-buster :as cache-buster :refer [wrap-resource]]
    [leihs.procurement.utils.json-protocol :refer [to-json]]
    [leihs.procurement.utils.url.core :as url]

    [clojure.java.jdbc :as jdbc]
    [hiccup.page :refer [include-js html5]]

    [clojure.tools.logging :as logging]
    [logbug.catcher :as catcher]
    [logbug.debug :as debug :refer [I>]]
    [logbug.ring :refer [wrap-handler-with-logging]]
    [logbug.thrown :as thrown]
    ))

; (defn include-site-css []
;   (hiccup.page/include-css
;     (cache-buster/cache-busted-path "/procurement/css/site.css")))

; (defn include-font-css []
;   (hiccup.page/include-css
;     "/procurement/css/fontawesome-free-5.0.1//css/fontawesome-all.css"))

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
   ; (include-site-css)
   ; (include-font-css)
   ])

(defn user-data [request]
  (url/encode
    (to-json
      (when-let [user-id (-> request :authenticated-entity :user_id)]
        (->> (user/user-query user-id)
             (jdbc/query (:tx request)) first)))))

(defn settings-data [request]
  (url/encode
    (to-json
      (-> request
          :settings
          (select-keys
            [:shibboleth_enabled
             :shibboleth_login_path])))))

(defn not-found-handler [request]
  {:status 404
   :headers {"Content-Type" "text/html"}
   :body (html5
           (head)
           [:body
            {:data-user (user-data request)
             :data-settings (settings-data request)}
            [:div.container-fluid
             [:h1.text-danger "Error 404 - Not Found"]]])})

(defn html-handler [request]
  {:headers {"Content-Type" "text/html"}
   :body (html5
           (head)
           [:body
            {:data-user (user-data request)
             :data-settings (settings-data request)}
            [:div#app.container-fluid
             [:div.alert.alert-warning
              [:h1 "Leihs Procurement 2"]
              [:p "OK"]]]
            ; (hiccup.page/include-js
            ;   (cache-buster/cache-busted-path "/procurement/js/app.js"))
            ])})


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns 'cider-ci.utils.shutdown)
;(debug/debug-ns *ns*)
