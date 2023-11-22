(ns leihs.admin.resources.buildings.building.create
  (:refer-clojure :exclude [str keyword])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.buildings.building.core :as building-core]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]))

(defn create []
  (go (when-let [id (some->
                     {:url (path :buildings)
                      :method :post
                      :json-params  @building-core/data*
                      :chan (async/chan)}
                     http-client/request :chan <!
                     http-client/filter-success!
                     :body :id)]
        (accountant/navigate!
         (path :building {:building-id id})))))

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  (reset! building-core/data* {})
  [:> Modal {:size "md"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Add Building"]]
   [:> Modal.Body
    [building-core/building-form create]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :onClick onHide}
     "Cancel"]
    [:> Button {:type "submit"
                :form "building-form"}
     "Save"]]])
