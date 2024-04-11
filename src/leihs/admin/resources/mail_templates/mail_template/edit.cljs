(ns leihs.admin.resources.mail-templates.mail-template.edit
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.mail-templates.mail-template.core :as mail-template-core]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]))

(defn patch []
  (let [route (path :mail-template
                    {:mail-template-id @mail-template-core/id*})]
    (go (when (some->
               {:url route
                :method :patch
                :json-params  @mail-template-core/data*
                :chan (async/chan)}
               http-client/request :chan <!
               http-client/filter-success!)
          (accountant/navigate! route)))))

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  [:> Modal {:size "xl"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Edit Mail Template"]]
   [:> Modal.Body
    [mail-template-core/form patch]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :onClick onHide}
     "Cancel"]
    [:> Button {:type "submit"
                :form "mail-template-form"}
     "Save"]]])
