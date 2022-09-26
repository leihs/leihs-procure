(ns leihs.admin.resources.rooms.room.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as core-user]
    [leihs.core.user.shared :refer [short-id]]

    [leihs.admin.common.components :as components]
    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.rooms.room.breadcrumbs :as breadcrumbs]
    [leihs.admin.state :as state]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(defonce id*
  (reaction (or (-> @routing/state* :route-params :room-id presence)
                ":room-id")))

(defonce data* (reagent/atom nil))

(defonce buildings-data* (reagent/atom nil))

(defonce edit-mode?*
  (reaction
    (and (map? @data*)
         (boolean ((set '(:room-edit :room-create))
                   (:handler-key @routing/state*))))))


;;; fetch ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clean-and-fetch [& args]
  (reset! data* nil)
  (reset! buildings-data* nil)
  (go (reset! buildings-data*
              (some-> {:chan (async/chan)
                       :url (path :buildings)}
                      http-client/request :chan <!
                      http-client/filter-success!
                      :body :buildings))
      (if (= (:route @routing/state*) (path :room-create))
        (reset! data* {:building_id nil #_(-> @buildings-data* first :id)})
        (reset! data*
                (some->
                  {:chan (async/chan)
                   :url (path :room
                              (-> @routing/state* :route-params))}
                  http-client/request :chan <!
                  http-client/filter-success! :body)))))

;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:<>
     [:div.room-debug
      [:hr]
      [:div.room-data
       [:h3 "@data*"]
       [:pre (with-out-str (pprint @data*))]]]
     [:div.room-debug
      [:hr]
      [:div.room-data
       [:h3 "@buildings-data*"]
       [:pre (with-out-str (pprint @buildings-data*))]]] ]))


;;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn id-component []
  [:p "id: " [:span {:style {:font-family "monospace"}} (:id @data*)]])

(defn name-link-component []
  [:span
   ; [routing/hidden-state-component
   ;  {:did-change fetch}]
   (let [p (path :room {:room-id @id*})
         inner (if @data*
                 [:em (str (:name @data*))]
                 [:span {:style {:font-family "monospace"}} (short-id @id*)])]
     [components/link inner p])])
