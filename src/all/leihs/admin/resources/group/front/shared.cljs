(ns leihs.admin.resources.group.front.shared
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.shared :refer [short-id]]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.shared :refer [humanize-datetime-component gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.jimp]
    [cljsjs.moment]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [clojure.string]
    [reagent.core :as reagent]
    ))

(defonce group-id* (reaction (-> @routing/state* :route-params :group-id)))
(defonce group-data* (reagent/atom nil))


(defonce edit-mode?*
  (reaction
    (and (map? @group-data*)
         (boolean ((set '(:group-edit :group-add))
                   (:handler-key @routing/state*))))))

(defn fetch-group []
  (defonce fetch-group-id* (reagent/atom nil))
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :group (-> @routing/state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Group"
                               :handler-key :group
                               :retry-fn #'fetch-group}
                              :chan resp-chan)]
    (reset! fetch-group-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-group-id*))
            (reset! group-data* (:body resp)))))))


;;; reload logic ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clean-and-fetch [& args]
  (reset! group-data* nil)
  (fetch-group))

(defn group-id-component []
  [:p "id: " [:span {:style {:font-family "monospace"}} (:id @group-data*)]])

(defn group-name-component []
  (if-not @group-data*
    [:span {:style {:font-family "monospace"}} (short-id @group-id*)]
    [:em (str (:name @group-data*))]))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.group-debug
     [:hr]
     [:div.edit-mode?*
      [:h3 "@edit-mode?*"]
      [:pre (with-out-str (pprint @edit-mode?*))]]
     [:div.group-data
      [:h3 "@group-data*"]
      [:pre (with-out-str (pprint @group-data*))]]]))

