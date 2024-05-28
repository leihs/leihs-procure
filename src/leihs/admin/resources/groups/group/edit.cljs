(ns leihs.admin.resources.groups.group.edit
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [go <!]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.groups.group.core :refer [data* group-id* clean-and-fetch]]
   [leihs.admin.resources.groups.group.edit-core :as edit-core]
   [react-bootstrap :as react-bootstrap :refer [Button Form Modal]]))

(defn patch []
  (go (when (some->
             {:chan (async/chan)
              :url (path :group {:group-id @group-id*})
              :method :patch
              :json-params @data*}
             http-client/request :chan <!
             http-client/filter-success!)
        (clean-and-fetch))))

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  [:> Modal {:size "xl"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Edit Group"]]
   [:> Modal.Body
    [:> Form {:id "add-user-form"
              :on-submit (fn [e]
                           (.preventDefault e)
                           (patch)
                           (onHide))}
     [edit-core/inner-form-component]]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :onClick onHide}
     "Cancel"]
    [:> Button {:type "submit"
                :form "add-user-form"}
     "Save"]]])
