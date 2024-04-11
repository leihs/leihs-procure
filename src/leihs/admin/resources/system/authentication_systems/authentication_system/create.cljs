(ns leihs.admin.resources.system.authentication-systems.authentication-system.create
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.system.authentication-systems.authentication-system.core :as auth-core]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]))

(defn create []
  (go (when-let [id (some->
                     {:chan (async/chan)
                      :url (path :authentication-systems)
                      :method :post
                      :json-params  @auth-core/data*}
                     http-client/request :chan <!
                     http-client/filter-success! :body :id)]
        (accountant/navigate!
         (path :authentication-system
               {:authentication-system-id id})))))

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  (reset! auth-core/data* {})
  [:> Modal {:size "lg"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Add an Authentication System"]]
   [:> Modal.Body
    [auth-core/form create]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :onClick onHide}
     "Cancel"]
    [:> Button {:type "submit"
                :form "auth-form"}
     "Save"]]])
