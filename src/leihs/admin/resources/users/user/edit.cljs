(ns leihs.admin.resources.users.user.edit
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.users.user.core :as user :refer [user-id*]]
   [leihs.admin.resources.users.user.edit-core :as edit-core :refer [data*]]
   [leihs.admin.resources.users.user.edit-image :as edit-image]
   [react-bootstrap :as react-bootstrap :refer [Button Form Modal]]))

(defn patch []
  (go (when (some->
             {:chan (async/chan)
              :url (path :user {:user-id @user-id*})
              :method :patch
              :json-params  (-> @data*
                                (update-in [:extended_info]
                                           (fn [s] (.parse js/JSON s))))}
             http-client/request :chan <!
             http-client/filter-success!)
        (user/clean-and-fetch))))

(defn inner-form-component []
  [:div
   [edit-core/essentials-form-component]
   [:div.image.mt-5
    [:h3 "Image / Avatar"]
    [edit-image/image-component]]
   [edit-core/personal-and-contact-form-component]
   [edit-core/account-settings-form-component]])

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  [:> Modal {:size "xl"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Edit User"]]
   [:> Modal.Body
    [:> Form {:id "add-user-form"
              :on-submit (fn [e]
                           (.preventDefault e)
                           (patch)
                           (onHide))}
     [inner-form-component]]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :onClick onHide}
     "Cancel"]
    [:> Button {:type "submit"
                :form "add-user-form"}
     "Save"]]])
