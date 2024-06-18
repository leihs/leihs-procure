(ns leihs.admin.resources.inventory-pools.inventory-pool.workdays.core
  (:require
   [cljs.core.async :as async :refer [go <! timeout]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent :refer [reaction]]))

(defonce DAYS {:monday :1
               :tuesday :2
               :wednesday :3
               :thursday :4
               :friday :5
               :saturday :6
               :sunday :0})

(defonce data* (reagent/atom nil))

(defn fetch []
  (go (<! (timeout 1))
      (reset! data* (some->
                     {:chan (async/chan)
                      :url (path :inventory-pool-workdays
                                 (-> @routing/state* :route-params))}
                     http-client/request :chan <!
                     http-client/filter-success! :body))))

(defn clean-and-fetch [& args]
  (reset! data* nil)
  (fetch))

