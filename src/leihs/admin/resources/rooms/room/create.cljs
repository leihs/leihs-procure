(ns leihs.admin.resources.rooms.room.create
  (:refer-clojure :exclude [str keyword])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.rooms.room.core :as room-core]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]))

(defn create []
  (go (when-let [id (some->
                     {:url (path :rooms)
                      :method :post
                      :json-params  @room-core/data*
                      :chan (async/chan)}
                     http-client/request :chan <!
                     http-client/filter-success!
                     :body :id)]
        (accountant/navigate!
         (path :room {:room-id id})))))

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  (reset! room-core/data* {})
  [:> Modal {:size "md"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Add a Room"]]
   [:> Modal.Body
    [room-core/room-form create]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :onClick onHide}
     "Cancel"]
    [:> Button {:type "submit"
                :form "room-form"}
     "Save"]]])
