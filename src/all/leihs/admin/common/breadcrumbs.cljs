(ns leihs.admin.common.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.breadcrumbs :as core-breadcrumbs]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.icons :as icons]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as core-user]
    [leihs.core.auth.core :as auth]

    [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
    [leihs.admin.paths :as paths :refer [path]]))

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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn email-li [address]
  [:li.breadcrumb-item {:key (str "mailto:" address )}
   [:a {:href (str "mailto:" address )} [:i.fas.fa-envelope] " Email "]])



