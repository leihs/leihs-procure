(ns leihs.admin.resources.main)

(defn page []
  [:article.admin.my-5
   [:div
    [:h1 "Admin"]
    [:p "The application to administrate this instance of "
     [:em " leihs"] "."]]])
