(ns leihs.admin.resources.users.user.api-tokens.delete
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [go <!]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]))

(defn delete []
  (go (when (some->
             {:chan (async/chan)
              :url (path :user-api-token (-> @routing/state* :route-params))
              :method :delete}
             http-client/request :chan <!
             http-client/filter-success!)
        (accountant/navigate!
         (path :user (-> @routing/state* :route-params))))))

(defn dialog [& {:keys [show on-hide]
                 :or {show false}}]
  [:> Modal {:size "sm"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide on-hide}
    [:> Modal.Title "Delete API Token"]]
   [:> Modal.Body
    "Are you sure you want to delete this api token"]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :onClick on-hide}
     "Cancel"]
    [:> Button {:variant "danger"
                :onClick #(do (on-hide)
                              (delete))}
     "Delete"]]])
