(ns leihs.admin.resources.leihs-root)

(defn page []
  [:div.home
   [:h1.my-5 "leihs-admin Home"]
   [:p.text-danger "This page is only accessible for development and testing."]])
