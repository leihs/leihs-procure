(ns leihs.admin.resources.settings.syssec.edit
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.resources.settings.syssec.core :as syssec-core]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]))

(defn put []
  (go (when (some->
             {:chan (async/chan)
              :json-params @syssec-core/data*
              :method :put}
             http-client/request :chan <!
             http-client/filter-success :body)
        (accountant/navigate! "/admin/settings/syssec/"))))

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  (syssec-core/fetch)
  [:> Modal {:size "lg"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Edit Miscellaneous"]]
   [:> Modal.Body
    [syssec-core/form put]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :onClick onHide}
     "Cancel"]
    [:> Button {:type "submit"
                :form "syssec-form"}
     "Save"]]])

