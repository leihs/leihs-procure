(ns leihs.admin.resources.inventory-fields.inventory-field.inventory-pools.core
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
                :url (path :inventory-field-inventory-pools (-> @routing/state* :route-params))}
               http/request :chan <!
               http/filter-success!
               :body :inventory-pools))))

(defn clean-and-fetch []
  (reset! data* nil)
  (fetch))
