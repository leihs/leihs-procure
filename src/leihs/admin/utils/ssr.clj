(ns leihs.admin.utils.ssr
  (:require [hiccup.page :refer [html5 include-js]]
            [leihs.core.shared :refer [head]]
            [leihs.core.http-cache-buster2 :as cache-buster]))

(defn render-page-base [inner-html]
  (html5
    (head
      (hiccup.page/include-css (cache-buster/cache-busted-path
                                 "/admin/css/theme/bootstrap-leihs.css")))
    [:body {:class "bg-paper"}
     [:noscript "This application requires Javascript."]
     inner-html
     (hiccup.page/include-js (cache-buster/cache-busted-path
                               "/admin/leihs-shared-bundle.js"))]))