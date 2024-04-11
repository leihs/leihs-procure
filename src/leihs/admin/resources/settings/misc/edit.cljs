(ns leihs.admin.resources.settings.misc.edit
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.resources.settings.misc.core :as misc-core]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]))

(defn put []
  (go (when-let [data (some->
                       {:chan (async/chan)
                        :json-params @misc-core/data*
                        :method :put}
                       http-client/request :chan <!
                       http-client/filter-success :body)]
        (accountant/navigate! "/admin/settings/misc/"))))

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  (misc-core/fetch)
  [:> Modal {:size "lg"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Edit Miscellaneous"]]
   [:> Modal.Body
    [misc-core/form put]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :onClick onHide}
     "Cancel"]
    [:> Button {:type "submit"
                :form "misc-form"}
     "Save"]]])
