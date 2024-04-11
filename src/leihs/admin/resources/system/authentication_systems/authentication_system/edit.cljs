(ns leihs.admin.resources.system.authentication-systems.authentication-system.edit
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.system.authentication-systems.authentication-system.core :as auth-core]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]))

(defn patch []
  (let [route (path :authentication-system
                    {:authentication-system-id @auth-core/id*})]
    (go (when (some->
               {:url route
                :method :patch
                :json-params  (dissoc @auth-core/data* :users_count)
                :chan (async/chan)}
               http-client/request :chan <!
               http-client/filter-success!)
          (accountant/navigate! route)))))

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  [:> Modal {:size "lg"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Edit Authentication System"]]
   [:> Modal.Body
    [auth-core/form patch]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :onClick onHide}
     "Cancel"]
    [:> Button {:type "submit"
                :form "auth-form"}
     "Save"]]])
