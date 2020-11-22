(ns leihs.admin.state
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.dom :as dom]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as current-user]
    [leihs.core.auth.core :as authorization]

    [clojure.pprint :refer [pprint]]
    [reagent.core :as reagent]
    [timothypratley.patchin :as patchin]

    ))

(defonce global-state* (reagent/atom {:debug false
                                      :users-query-params {}
                                      :timestamp (js/moment)}))

(js/setInterval #(swap! global-state*
                       (fn [s] (merge s {:timestamp (js/moment)}))) 1000)


(def leihs-admin-version* (reagent/atom (dom/data-attribute "body" "leihsadminversion")))

(def leihs-version (dom/data-attribute "body" "leihsversion"))


(def debug?* (reaction (:debug @global-state*)))

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
                        :style {:padding-left "0.25em"}} " debug "]])

(defn debug-component []
  (when (:debug @global-state*)
    [:div.debug
     [:hr]
     [:h2 "Debug State"]
     [:div
      [:h3 "@global-state*"]
      [:pre (with-out-str (pprint @global-state*))]]
     [:div
      [:h3 "@leihs-admin-version"]
      [:pre (with-out-str (pprint @leihs-admin-version*))]]
     [:div
      [:h3 "@routing/state*"]
      [:pre (with-out-str (pprint @routing/state*))]]
     [:div
      [:h3 "@current-user/state*"]
      [:pre (with-out-str (pprint @current-user/state*))]]
     [:div
      [:h3 "(authorization/admin-scopes? @current-user/state*)"]
      [:pre (with-out-str (pprint  (authorization/admin-scopes?
                                     @current-user/state* @routing/state*)))]]
     [:div
      [:h3 "(authorization/system-admin-scopes? @current-user/state*)"]
      [:pre (with-out-str (pprint (authorization/system-admin-scopes?
                                    @current-user/state* @routing/state*)))]]]))
