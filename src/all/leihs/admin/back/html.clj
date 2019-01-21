(ns leihs.admin.back.html
  (:require [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [leihs.admin.env :refer [env]]
            [hiccup.page :refer [html5 include-js]]
            [environ.core :refer [env] :rename {env system-env}]
            [leihs.admin.paths :refer [path]]
            [leihs.admin.utils.release-info :as release-info]
            [leihs.core
             [http-cache-buster2 :as cache-buster]
             [json :refer [to-json]]]
            [leihs.core.url.core :as url]))

(defn include-site-css []
  (hiccup.page/include-css
    (cache-buster/cache-busted-path "/admin/css/site.css")))

(defn include-font-css []
  (hiccup.page/include-css
    "/admin/css/fontawesome-free-5.0.13/css/fontawesome-all.css"))

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
   (include-site-css)
   (include-font-css)])

(defn body-attributes [request]
  {:data-remote-navbar (-> system-env :leihs-remote-navbar-url nil? not str)
   :data-user  (some-> (:authenticated-entity request) to-json url/encode)
   :data-leihsadminversion (url/encode (to-json release-info/leihs-admin-version))
   :data-leihsversion (url/encode (to-json release-info/leihs-version))})

(defn not-found-handler [request]
  {:status 404
   :headers {"Content-Type" "text/html"}
   :body (html5
           (head)
           [:body
            (body-attributes request)
            [:div.container-fluid
             [:h1.text-danger "Error 404 - Not Found"]]])})

(defn html-handler [request]
  (let [remote-navbar
        (if-let [remote-navbar-url (:leihs-remote-navbar-url system-env)]
          (-> remote-navbar-url
              (client/get
                (select-keys request [:cookies]))
              :body))]
    {:headers {"Content-Type" "text/html"}
     :body (html5
             (head)
             [:body
              (body-attributes request)
              [:div
               remote-navbar
               [:div#app.container-fluid
                [:div.alert.alert-warning
                 [:h1 "Leihs Admin2"]
                 [:p "This application requires Javascript."]]]]
              (hiccup.page/include-js
                (cache-buster/cache-busted-path "/admin/js/app.js"))])}))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
