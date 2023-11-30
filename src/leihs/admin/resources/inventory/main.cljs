(ns leihs.admin.resources.inventory.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [clojure.string :as str]
   [leihs.admin.common.icons :as icons]

   [leihs.admin.paths :as paths :refer [path paths]]
   [leihs.admin.resources.inventory.breadcrumbs :as breadcrumbs]

   [leihs.core.core :refer [keyword str presence]]))

(defn page []
  [:div.page.inventory-page
   [breadcrumbs/nav-component @breadcrumbs/left* []]
   [:h1 [icons/inventory] " Inventory"]
   [:h2 "Export / Download Inventory"]
   [:ul
    [:li [:a {:href (path :inventory-csv) :target :_blank} [:i.fas.fa-download] " CSV"]]
    [:li [:a {:href (path :inventory-quick-csv) :target :_blank} [:i.fas.fa-download] " Quick-CSV"]]
    [:li [:a {:href (path :inventory-excel) :target :_blank} [:i.fas.fa-download] " Excel"]]
    [:li [:a {:href (path :inventory-quick-excel) :target :_blank} [:i.fas.fa-download] " Quick-Excel"]]]])
