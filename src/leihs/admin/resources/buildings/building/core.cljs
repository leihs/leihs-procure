(ns leihs.admin.resources.buildings.building.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [timeout]]
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components :as components]
   [leihs.admin.common.http-client.core :as http-client]

   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.buildings.building.breadcrumbs :as breadcrumbs]
   [leihs.admin.state :as state]
   [leihs.core.core :refer [keyword str presence]]

   [leihs.core.routing.front :as routing]
   [leihs.core.user.front :as core-user]
   [leihs.core.user.shared :refer [short-id]]
   [reagent.core :as reagent]))

(defonce id*
  (reaction (or (-> @routing/state* :route-params :building-id presence)
                ":building-id")))

(defonce data* (reagent/atom nil))

(defonce edit-mode?*
  (reaction
   (and (map? @data*)
        (boolean ((set '(:building-edit :building-create))
                  (:handler-key @routing/state*))))))

;;; fetch ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch []
  (go (reset! data*
              (some->
               {:chan (async/chan)
                :url (path :building
                           (-> @routing/state* :route-params))}
               http-client/request :chan <!
               http-client/filter-success! :body))))

(defn clean-and-fetch [& args]
  (reset! data* nil)
  (fetch))

;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.building-debug
     [:hr]
     [:div.building-data
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

;;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn id-component []
  [:p "id: " [:span {:style {:font-family "monospace"}} (:id @data*)]])

(defn name-link-component []
  [:span
   [routing/hidden-state-component
    {:did-change fetch}]
   (let [p (path :building {:building-id @id*})
         inner (if @data*
                 [:em (str (:name @data*))]
                 [:span {:style {:font-family "monospace"}} (short-id @id*)])]
     [components/link inner p])])
