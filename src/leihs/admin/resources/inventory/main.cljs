(ns leihs.admin.resources.inventory.main
  (:require
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]))

(defn page []
  [:article.page.inventory-page
   [:header.my-5
    [:h1 [icons/file-export] " Inventory"]]
   [:section
    [:h2 "Export / Download Inventory"]
    [:ul
     [:li [:a {:href (path :inventory-csv) :target :_blank} [:i.fas.fa-download] " CSV"]]
     [:li [:a {:href (path :inventory-quick-csv) :target :_blank} [:i.fas.fa-download] " Quick-CSV"]]
     [:li [:a {:href (path :inventory-excel) :target :_blank} [:i.fas.fa-download] " Excel"]]
     [:li [:a {:href (path :inventory-quick-excel) :target :_blank} [:i.fas.fa-download] " Quick-Excel"]]]]])
