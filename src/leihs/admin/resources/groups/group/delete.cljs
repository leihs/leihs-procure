(ns leihs.admin.resources.groups.group.delete
  (:refer-clojure :exclude [str keyword])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [go <!]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]))

;;; delete ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn post []
  (go (when (some->
             {:chan (async/chan)
              :url (path :group (-> @routing/state* :route-params))
              :method :delete}
             http-client/request :chan <!
             http-client/filter-success!)
        (accountant/navigate!
         (path :groups {})))))

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  [:> Modal {:size "sm"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Delete Group"]]
   [:> Modal.Body
    "Are you sure you want to delete this group?"]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :onClick onHide}
     "Cancel"]
    [:> Button {:variant "danger"
                :onClick #(do (onHide)
                              (post))}
     "Delete"]]])
