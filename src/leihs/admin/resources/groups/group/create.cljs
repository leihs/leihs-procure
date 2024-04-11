(ns leihs.admin.resources.groups.group.create
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.groups.group.core :refer [data*]]
   [leihs.admin.resources.groups.group.edit-core :as edit-core]
   [react-bootstrap :as react-bootstrap :refer [Button Form Modal]]))

(defn post []
  (go (when-let [body (some->
                       {:chan (async/chan)
                        :url (path :groups)
                        :method :post
                        :json-params @data*}
                       http-client/request
                       :chan <! http-client/filter-success!
                       :body)]
        (accountant/navigate!
         (path :group {:group-id (:id body)})))))

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  (reset! data* {})
  [:> Modal {:size "xl"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Add a new Group"]]
   [:> Modal.Body
    [:> Form {:id "add-group-form"
              :on-submit (fn [e] (.preventDefault e) (post))}
     [edit-core/inner-form-component]]]
   [:> Modal.Footer
    [:> Button {:variant "secondary" :onClick onHide}
     "Cancel"]
    [:> Button {:type "submit"
                :form "add-group-form"}
     "Add"]]])
