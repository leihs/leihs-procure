(ns leihs.admin.resources.settings.syssec.core
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.form-components :as form-components]
   [leihs.admin.common.http-client.core :as http-client]
   [react-bootstrap :as react-bootstrap :refer [Col Form Row]]
   [reagent.core :as reagent]))

(defonce data* (reagent/atom nil))

(defn fetch []
  (go (reset! data*
              (some-> {:chan (async/chan)}
                      http-client/request
                      :chan <! http-client/filter-success! :body))))

(defn form [action data*]
  [:> Form
   {:id "syssec-form"
    :on-submit (fn [e] (.preventDefault e) (action))}
   [:div
    [form-components/input-component data* [:external_base_url]
     :label "Base URL"]]
   [:div
    [form-components/input-component data* [:instance_element]
     :rows 3 :element :textarea
     :hint [:span "Some custom html/text. "]]]

   [:> Row
    [:> Col
     [form-components/input-component data* [:sessions_max_lifetime_secs]
      :type :number]]
    [:> Col
     [form-components/checkbox-component data* [:sessions_force_secure]]]
    [:> Col
     [form-components/checkbox-component data* [:sessions_force_uniqueness]]]]

   [:> Row
    [:> Col
     [form-components/checkbox-component data* [:public_image_caching_enabled]
      :hint [:div
             [:p (str "Sets http-headers such that images are treated as public available resources. "
                      "This enables caching of the images at various stages. "
                      "It does not expose the images to crawlers since the listing of the images is not public! ")]
             [:p (str "We recommend leave this setting enabled as it generally improves user experience and lifts load from the application server. ")]]]]]])
