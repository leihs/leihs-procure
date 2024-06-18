(ns leihs.admin.resources.users.user.edit
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.users.user.core :as core :refer [user-id*]]
   [leihs.admin.resources.users.user.edit-core :as edit-core]
   [leihs.admin.resources.users.user.edit-image :as edit-image]
   [leihs.admin.resources.users.user.inventory-pools :as user-inventory-pools]
   [leihs.admin.utils.search-params :as search-params]
   [leihs.core.auth.core :as auth]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button Form Modal]]
   [reagent.core :as reagent :refer [reaction]]))

(defonce data* (reagent/atom nil))

(defn patch []
  (go (when-let [res (some->
                      {:chan (async/chan)
                       :url (path :user {:user-id @user-id*})
                       :method :patch
                       :json-params  (-> @data*
                                         (update-in [:extended_info]
                                                    (fn [s] (.parse js/JSON s))))}
                      http-client/request :chan <!
                      http-client/filter-success! :body)]
        (search-params/delete-from-url "action")
        (user-inventory-pools/clean-and-fetch)
        (reset! core/user-data* res))))

(defn inner-form-component [data*]
  [:<>
   [edit-core/essentials-form-component data*]
   [:div.image.mt-5
    [:h3 "Image / Avatar"]
    [edit-image/image-component data*]]
   [edit-core/personal-and-contact-form-component data*]
   [edit-core/account-settings-form-component data*]])

(def open*
  (reaction
   (reset! data* @core/user-data*)
   (->> (:query-params @routing/state*)
        (:action)
        (= "edit"))))

(defn dialog []
  [:> Modal {:size "xl"
             :centered true
             :scrollable true
             :show @open*}
   [:> Modal.Header {:close-button true

                     :on-hide #(search-params/delete-from-url "action")}
    [:> Modal.Title {:class-name "edit-user-modal"}
     "Edit User"]]
   [:> Modal.Body
    [:> Form {:id "edit-user-form"
              :on-submit (fn [e]
                           (.preventDefault e)
                           (patch))}
     [inner-form-component data*]]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :on-click #(search-params/delete-from-url "action")}
     "Cancel"]
    [:> Button {:type "submit"
                :form "edit-user-form"}
     "Save"]]])

(defn button []
  (when (auth/allowed? [core/modifieable?])
    [:<>
     [:> Button
      {:on-click #(search-params/append-to-url
                   {:action "edit"})}
      "Edit User"]]))
