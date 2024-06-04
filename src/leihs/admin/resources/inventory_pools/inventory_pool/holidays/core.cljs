(ns leihs.admin.resources.inventory-pools.inventory-pool.holidays.core
  (:require
   [cljs.core.async :as async :refer [go <!]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent :refer [reaction]]))

(defonce data* (reagent/atom nil))

(defn fetch []
  (go (reset! data* (some->
                     {:chan (async/chan)
                      :url (path :inventory-pool-holidays
                                 (-> @routing/state* :route-params))}
                     http-client/request :chan <!
                     http-client/filter-success! :body))))

(defn clean-and-fetch [& args]
  (reset! data* nil)
  (fetch))
