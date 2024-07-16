(ns leihs.admin.resources.mail-templates.mail-template.core
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.state :as state]
   [leihs.core.core :refer [presence]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Col Form Row]]
   [reagent.core :as reagent]
   [reagent.ratom :as ratom :refer [reaction]]))

(defonce id*
  (reaction (or (-> @routing/state* :route-params :mail-template-id presence)
                ":mail-template-id")))

(defonce path*
  (reaction
   (path :mail-template {:mail-template-id @id*})))

(defonce cache* (reagent/atom nil))
(defonce data*
  (reaction (get @cache* @path*)))

(defn fetch []
  (http-client/route-cached-fetch cache* {:route @path*}))

(def template-variables-for-order-component
  [:ul
   [:li [:code :comment]],
   [:li [:code :email_signature]],
   [:li [:code :inventory_pool.name]],
   [:li [:code :inventory_pool.description]],
   [:li [:code :order_url]]
   [:li [:code :purpose]],
   [:li [:code :reservations]
    [:ul
     [:li [:code :l.end_date]]
     [:li [:code :l.model_name]],
     [:li [:code :l.quantity]],
     [:li [:code :l.start_date]]]],
   [:li [:code :user.name]]])

(def template-variables-for-user-component
  [:ul
   [:li [:code :due_date]],
   [:li [:code :email_signature]],
   [:li [:code :inventory_pool.name]],
   [:li [:code :inventory_pool.description]],
   [:li [:code :quantity]],
   [:li [:code :reservations]
    [:ul
     [:li [:code :l.end_date]]
     [:li [:code :l.item_inventory_code]]
     [:li [:code :l.model_name]],
     [:li [:code :l.quantity]],
     [:li [:code :l.start_date]]]],
   [:li [:code :user.name]]])

(defn template-variables []
  (case (:type @data* "order")
    "order" template-variables-for-order-component
    "user" template-variables-for-user-component
    nil))

(defn form [action data*]
  [:> Row
   [:> Col {:md 12 :lg 8}
    [:> Form {:id "mail-template-form"
              :on-submit (fn [e] (.preventDefault e) (action))}
     [:> Form.Group {:control-id "body"}
      [:> Form.Label "Mail Body"]
      [:textarea.form-control
       {:id "body"
        :rows 30
        :required true
        :value (or (:body @data*) "")
        :onChange (fn [e] (swap! data* assoc :body (-> e .-target .-value)))}]]]]
   [:> Col
    [:div "Variables"
     (template-variables)]]])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.mail-template-debug
     [:hr]
     [:div.mail-template-data
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))
