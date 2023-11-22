(ns leihs.admin.resources.system.authentication-systems.authentication-system.delete
  (:refer-clojure :exclude [str keyword])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]))

(defn post []
  (go (when (some->
             {:url (path :authentication-system
                         (-> @routing/state* :route-params))
              :method :delete
              :chan (async/chan)}
             http-client/request :chan <!
             http-client/filter-success!)
        (accountant/navigate!
         (path :authentication-systems)))))

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  [:> Modal {:size "md"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Delete Authentication System"]]
   [:> Modal.Body
    "Are you sure you want to delete this Authentication System?"
    [:p.font-weight-bold.pt-3 "This action cannot be undone."]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :onClick onHide}
     "Cancel"]
    [:> Button {:variant "danger"
                :onClick #(do
                            (onHide)
                            (post))}
     "Delete"]]])

