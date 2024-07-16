(ns leihs.admin.resources.groups.group.core
  (:require
   [cljs.core.async :as async :refer [<! go timeout]]
   [cljs.pprint :refer [pprint]]
   [clojure.string]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.admin.state :as state]
   [leihs.core.auth.core :as auth]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent :refer [reaction]]))

(defonce group-id* (reaction (-> @routing/state* :route-params :group-id)))

(def path*
  (reaction
   (path :group {:group-id @group-id*})))

(def cache* (reagent/atom nil))

(def data*
  (reaction (get @cache* @path*)))

(defn some-lending-manager-and-group-unprotected? [current-user-state _]
  (and (pool-auth/some-lending-manager? current-user-state _)
       (boolean  @data*)
       (and (-> @data* :admin_protected not)
            (-> @data* :system_admin_protected not))))

(defn admin-and-group-not-system-admin-protected?
  [current-user routing-state]
  (and (auth/admin-scopes? current-user routing-state)
       (-> @data* :system_admin_protected not)))

(defn fetch []
  (http-client/route-cached-fetch cache* {:route @path*}))

(defn group-name-component []
  [:<> (:name @data*)])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.group-debug
     [:hr]
     [:div.group-data
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

