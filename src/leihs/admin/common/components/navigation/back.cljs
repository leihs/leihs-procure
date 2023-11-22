(ns leihs.admin.common.components.navigation.back
  (:require
   [accountant.core :as accountant]
   [leihs.admin.common.icons :as icons]
   [react-bootstrap :as react-bootstrap]))

(defn button [& {:keys [href]
                 :or {href false}}]
  [:> react-bootstrap/Button
   {:variant "outline-primary"
    :onClick (fn [& _] (if href
                         (accountant/navigate! href)
                         (js/history.back)))}
   [icons/back] " Back"])
