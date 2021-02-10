(ns leihs.admin.resources.groups.group.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.shared :refer [short-id]]
    [leihs.admin.common.http-client.core :as http-client]

    [leihs.admin.common.breadcrumbs :as breadcrumbs]
    [leihs.admin.common.components :as components]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.string]
    [reagent.core :as reagent]
    ))

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

(defn group-id-component []
  [:p "id: " [:span {:style {:font-family "monospace"}} (:id @data*)]])

(defn group-name-component []
  (let [inner (if-not @data*
                [:span {:style {:font-family "monospace"}} (short-id @group-id*)]
                [:em (str (:name @data*))])
        p (path :group {:group-id @group-id*})]
    [components/link inner p]))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.group-debug
     [:hr]
     [:div.group-data
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

