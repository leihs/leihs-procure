(ns leihs.admin.resources.system.authentication-systems.authentication-system.delete
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.utils.search-params :as search-params]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]
   [reagent.core :refer [reaction]]))

(defn post []
  (go (when (some->
             {:url (path :authentication-system
                         (-> @routing/state* :route-params))
              :method :delete
              :chan (async/chan)}
             http-client/request :chan <!
             http-client/filter-success!)
        (search-params/delete-from-url "action")
        (accountant/navigate!
         (path :authentication-systems)))))

(def open?*
  (reaction
   (->> (:query-params @routing/state*)
        :action
        (= "delete"))))

(defn dialog []
  [:> Modal {:size "md"
             :centered true
             :scrollable true
             :show @open?*}
   [:> Modal.Header {:closeButton true
                     :on-hide #(search-params/delete-from-url
                                "action")}
    [:> Modal.Title "Delete Authentication System"]]
   [:> Modal.Body
    "Are you sure you want to delete this Authentication System?"
    [:p.font-weight-bold.pt-3 "This action cannot be undone."]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :on-click #(search-params/delete-from-url
                            "action")}
     "Cancel"]
    [:> Button {:variant "danger"
                :on-click #(post)}
     "Delete"]]])

(defn button []
  [:> Button
   {:variant "danger"
    :class-name "ml-3"
    :on-click #(search-params/append-to-url
                {:action "delete"})}
   "Delete"])
