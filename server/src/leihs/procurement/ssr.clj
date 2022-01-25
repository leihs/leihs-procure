(ns leihs.procurement.ssr
  (:require [hiccup.page :refer [html5 include-js]]
            [leihs.core.shared :refer [head]]
            [leihs.core.http-cache-buster2 :as cache-buster]
            [taoensso.timbre :refer [debug info warn error]]
            ))

(defn render-page-base [inner-html]
  (warn "CHECK usage fontawesome css")
  (html5
    (head
      (hiccup.page/include-css (cache-buster/cache-busted-path
                                 "/procure/theme/bootstrap-leihs.css")))
    [:body {:class "bg-paper"}
     [:noscript "This application requires Javascript."]
     inner-html
     (hiccup.page/include-js (cache-buster/cache-busted-path
                               "/procure/leihs-shared-bundle.js"))]))
