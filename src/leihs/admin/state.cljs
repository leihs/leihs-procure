(ns leihs.admin.state
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [clojure.pprint :refer [pprint]]
   [leihs.core.auth.core :as authorization]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.dom :as dom]
   [leihs.core.routing.front :as routing]
   [leihs.core.user.front :as current-user]
   [reagent.core :as reagent]
   [timothypratley.patchin :as patchin]))

(defonce global-state* (reagent/atom {:debug false
                                      :users-query-params {}
                                      :server-state (dom/data-attribute "body" "server-state")
                                      :timestamp (js/Date.)}))

(js/setInterval #(swap! global-state*
                        (fn [s] (merge s {:timestamp (js/Date.)}))) 1000)

(def debug?*
  (reaction
   (or (:debug @global-state*)
       (-> @routing/state* :query-params :debug))))

(defn update-state [state-ref key-seq fun]
  (swap! state-ref
         (fn [cs]
           (assoc-in cs key-seq
                     (fun (get-in cs key-seq nil))))))

(defn debug-toggle-navbar-component []
  [:form.form-inline
   [:input#toggle-debug
    {:type :checkbox
     :checked (-> @global-state* :debug boolean)
     :on-change #(update-state global-state*
                               [:debug]
                               (fn [v] (not v)))}]
   [:label.navbar-text {:for "toggle-debug"
                        :style {:padding-left "0.25em"}} " Debug "]])

(defn debug-component []
  (when (:debug @global-state*)
    [:div.debug
     [:hr]
     [:h2 "Debug State"]
     [:div
      [:h3 "@global-state*"]
      [:pre (with-out-str (pprint @global-state*))]]
     [:div
      [:h3 "@routing/state*"]
      [:pre (with-out-str (pprint @routing/state*))]]
     [:div
      [:h3 "@current-user/state*"]
      [:pre (with-out-str (pprint @current-user/state*))]]
     [:div
      [:h3 "(authorization/leihts-admin-scopes? @current-user/state*)"]
      [:pre (with-out-str (pprint  (authorization/admin-scopes?
                                    @current-user/state* @routing/state*)))]]
     [:div
      [:h3 "(authorization/system-admin-scopes? @current-user/state*)"]
      [:pre (with-out-str (pprint (authorization/system-admin-scopes?
                                   @current-user/state* @routing/state*)))]]]))
