(ns leihs.admin.resources.groups.group.core
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [cljs.pprint :refer [pprint]]
   [clojure.string]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.state :as state]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent :refer [reaction]]))

(defonce group-id* (reaction (-> @routing/state* :route-params :group-id)))

(def data* (reagent/atom nil))

(def route* (reaction (path :group (:route-params @routing/state*))))

(defn fetch-group []
  (go (reset! data*
              (some-> {:chan (async/chan)
                       :url @route*}
                      http-client/request
                      :chan <! http-client/filter-success! :body))))

;;; reload logic ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clean-and-fetch [& args]
  (reset! data* nil)
  (fetch-group))

(defn group-name-component []
  [:<> (:name @data*)])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.group-debug
     [:hr]
     [:div.group-data
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

