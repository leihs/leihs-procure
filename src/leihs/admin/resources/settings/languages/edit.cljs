(ns leihs.admin.resources.settings.languages.edit
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.resources.settings.languages.core :as languages-core]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]))

(defn put []
  (go (when (some->
             {:chan (async/chan)
              :json-params @languages-core/data*
              :method :put}
             http-client/request :chan <!
             http-client/filter-success :body)
        (accountant/navigate! "/admin/settings/languages/"))))

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  (languages-core/fetch)
  [:> Modal {:size "lg"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Edit Languages"]]
   [:> Modal.Body
    [languages-core/form put {:is-editable true}]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :onClick onHide}
     "Cancel"]
    [:> Button {:type "submit"
                :form "languages-form"}
     "Save"]]])
