(ns leihs.admin.resources.users.user.create
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.admin.resources.users.user.core :as core]
   [leihs.admin.resources.users.user.edit :as edit]
   [leihs.admin.utils.search-params :as search-params]
   [leihs.core.auth.core :as auth]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button Form Modal]]
   [reagent.core :as reagent :refer [reaction]]))

(def data* (reagent/atom nil))

(defn post []
  (go (when-let [data (some->
                       {:chan (async/chan)
                        :url (path :users)
                        :method :post
                        :json-params  (-> @data*
                                          (update-in [:extended_info]
                                                     (fn [s] (.parse js/JSON s))))}
                       http-client/request :chan <!
                       http-client/filter-success! :body)]
        (search-params/delete-from-url "action")
        (accountant/navigate!
         (path :user {:user-id (:id data)})))))

(def open*
  (reaction
   (reset! data* @core/user-data*)
   (->> (:query-params @routing/state*)
        :action
        (= "add"))))

(defn dialog []
  [:> Modal {:size "xl"
             :centered true
             :scrollable true
             :show @open*}

   [:> Modal.Header {:close-button true
                     :on-hide #(search-params/delete-from-url
                                "action")}
    [:> Modal.Title "Add a new User"]]

   [:> Modal.Body
    [:> Form {:id "add-user-form"
              :on-submit (fn [e]
                           (.preventDefault e)
                           (post))}
     [edit/inner-form-component data*]]]

   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :on-click #(search-params/delete-from-url
                            "action")}
     "Cancel"]

    [:> Button {:type "submit"
                :form "add-user-form"}
     "Add"]]])

(defn button []
  (when (auth/allowed? [auth/admin-scopes?
                        pool-auth/some-lending-manager?])
    [:<>
     [:> Button
      {:className "ml-3"
       :onClick #(search-params/append-to-url
                  {:action "add"})}
      "Add User"]]))
