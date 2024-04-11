(ns leihs.admin.common.breadcrumbs
  (:require [leihs.core.breadcrumbs :as core-breadcrumbs]))

(def li core-breadcrumbs/li)

;;; nav-component ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn nav-component [lefts rights]
  [:div.row.nav-component.mt-3.breadcrumbs-bar
   [:nav.col-lg {:key :nav-left :aria-label :breadcrumb :role :navigation}
    (when (seq lefts)
      [:ol.breadcrumb
       (doall (map-indexed (fn [idx item] [:<> {:key idx} item]) lefts))])]
   [:nav.col-lg.breadcrumbs-right
    {:key :nav-right :role :navigation}
    (when (seq rights)
      [:ol.breadcrumb.leihs-nav-right
       (doall (map-indexed (fn [idx item] [:<> {:key idx} item]) rights))])]])
