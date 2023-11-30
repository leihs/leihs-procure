(ns leihs.admin.common.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.core.auth.core :as auth]
   [leihs.core.breadcrumbs :as core-breadcrumbs]
   [leihs.core.core :refer [keyword str presence]]

   [leihs.core.routing.front :as routing]
   [leihs.core.user.front :as core-user]))

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
  [core-breadcrumbs/li
   (str "mailto:" address)
   [:span [icons/email] " E-Mail"]
   {} {} :button true])
