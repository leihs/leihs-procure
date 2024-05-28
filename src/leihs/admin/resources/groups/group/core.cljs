(ns leihs.admin.resources.groups.group.core
  (:require
   [cljs.core.async :as async :refer [go timeout <!]]
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

(defonce fetch-id* (atom nil))

(defn fetch-group []
  (go (reset! data*
              (some-> {:chan (async/chan)
                       :url @route*}
                      http-client/request
                      :chan <! http-client/filter-success! :body))))

;;; reload logic ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; (defn clean-and-fetch [& args]
;;   (reset! data* nil)
;;   (fetch-group))

(defn clean-and-fetch [& _]
  (reset! data* nil)
  (let [fetch-id (reset! fetch-id* (rand-int (js/Math.pow 2 16)))]
    (go (<! (timeout 50))
        (when (= @fetch-id* fetch-id)
          (reset! data*
                  (-> {:chan (async/chan)
                       :url @route*}
                      http-client/request
                      :chan <! http-client/filter-success! :body))))))

(defn group-name-component []
  [:<> (:name @data*)])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.group-debug
     [:hr]
     [:div.group-data
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

