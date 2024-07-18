(ns leihs.admin.resources.inventory-pools.inventory-pool.fields.core
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent]))

(defonce data* (reagent/atom nil))

(defn fetch []
  (go (reset! data*
              (some->
               {:chan (async/chan)
                :url (path :inventory-pool-fields (-> @routing/state* :route-params))}
               http/request :chan <!
               http/filter-success!
               :body :fields))))

(defn clean-and-fetch []
  (reset! data* nil)
  (fetch))

; helper

(defn format-target-type [target-type]
  (case target-type
    "item" "Item"
    "license" "License"
    nil "Item+License"
    "n/a"))
