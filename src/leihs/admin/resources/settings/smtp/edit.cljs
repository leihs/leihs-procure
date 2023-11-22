(ns leihs.admin.resources.settings.smtp.edit
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.resources.settings.smtp.core :as smtp-core]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]))

(defn put [& _]
  (go (when (some->
             {:chan (async/chan)
              :json-params @smtp-core/data*
              :method :put}
             http-client/request :chan <!
             http-client/filter-success :body)
        (accountant/navigate! "/admin/settings/smtp/"))))

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  (smtp-core/fetch)
  [:> Modal {:size "lg"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Edit SMTP"]]
   [:> Modal.Body
    [smtp-core/form put]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :onClick onHide}
     "Cancel"]
    [:> Button {:type "submit"
                :form "smtp-form"}
     "Save"]]])

