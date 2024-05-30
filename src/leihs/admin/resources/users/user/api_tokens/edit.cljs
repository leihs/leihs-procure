(ns leihs.admin.resources.users.user.api-tokens.edit
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.users.user.api-tokens.core :as core]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]))

(defn patch [callback]
  (go (when (some->
             {:chan (async/chan)
              :url (path :user-api-token (-> @routing/state* :route-params))
              :method :patch
              :json-params @core/data*}
             http-client/request
             :chan <! http-client/filter-success!)
        (callback))))

(defn dialog [& {:keys [show on-hide on-save-callback]
                 :or {show false}}]
  [:> Modal {:size "xl"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide on-hide}
    [:> Modal.Title "Add API Token"]]
   [:> Modal.Body
    [core/form {:on-save #(patch on-save-callback)}]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :onClick on-hide}
     "Cancel"]
    [:> Button {:type "submit"
                :form "api-token-form"}
     "Save"]]])