(ns leihs.admin.resources.users.user.api-tokens.create
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.users.user.api-tokens.core :as core]
   [leihs.admin.resources.users.user.core :as user-core]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]
   [reagent.core :as reagent]))

(def saved-data* (reagent/atom nil))

(defn create []
  (go (when-let [saved-data
                 (some->
                  {:url (path :user-api-tokens {:user-id @user-core/user-id*})
                   :method :post
                   :json-params @core/data*
                   :chan (async/chan)}
                  http-client/request :chan <!
                  http-client/filter-success!
                  :body)]
        (reset! saved-data* saved-data))))

(defn confirm []
  (let [url (path :user-api-token {:user-id @user-core/user-id* :api-token-id (-> @saved-data* :id)})]
    (reset! saved-data* nil)
    (accountant/navigate!
     url)))

(defn confirmation-component []
  [:<>
   [:div.text-success
    [:h4 "The new API-Token "
     [:code.token_part (-> @saved-data* :token_part)]
     " has been added"]]
   [:div.bg-warning
    [:h4.text-center
     [:code.token_secret (-> @saved-data* :token_secret)]]
    [:p
     "The full token-secret is shown here once and only once. "
     "Only the first 5 letters will be stored and shown as a identifier. "]]])

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  [:> Modal {:size "xl"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Add API Token"]]
   [:> Modal.Body
    (if @saved-data*
      [confirmation-component]
      [core/form {:on-save create}])]
   [:> Modal.Footer
    (if @saved-data*
      [:> Button {:variant "primary"
                  :onClick confirm}
       " Continue "]
      [:<>
       [:> Button {:variant "secondary"
                   :onClick onHide}
        "Cancel"]
       [:> Button {:type "submit"
                   :form "api-token-form"}
        "Save"]])]])
