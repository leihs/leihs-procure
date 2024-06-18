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

(def data* (reagent/atom nil))

(def route* (reaction (path :group (:route-params @routing/state*))))

(defn some-lending-manager-and-group-unprotected? [current-user-state _]
  (and (pool-auth/some-lending-manager? current-user-state _)
       (boolean  @data*)
       (and (-> @data* :admin_protected not)
            (-> @data* :system_admin_protected not))))

(defn admin-and-group-not-system-admin-protected?
  [current-user routing-state]
  (and (auth/admin-scopes? current-user routing-state)
       (-> @data* :system_admin_protected not)))

(defonce fetch-id* (atom nil))

(defn fetch []
  (go (reset! data*
              (some-> {:chan (async/chan)
                       :url @route*}
                      http-client/request
                      :chan <! http-client/filter-success! :body))))

;;; reload logic ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (defn fetch [& _]
;;   (let [fetch-id (reset! fetch-id* (rand-int (js/Math.pow 2 16)))]
;;     (go (<! (timeout 50))
;;         (when (= @fetch-id* fetch-id)
;;           (reset! data*
;;                   (-> {:chan (async/chan)
;;                        :url @route*}
;;                       http-client/request
;;                       :chan <! http-client/filter-success! :body))))))

(defn clean-and-fetch []
  (reset! data* nil)
  (fetch))

(defn group-name-component []
  [:<> (:name @data*)])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.group-debug
     [:hr]
     [:div.group-data
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

