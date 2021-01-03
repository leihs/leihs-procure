(ns leihs.admin.resources.status.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.anti-csrf.front :as anti-csrf]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.resources.status.breadcrumbs :as breadcrumbs]

    [leihs.admin.common.components :as components]
    [leihs.core.icons :as icons]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]

    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce status-info-data* (reagent/atom nil))

(def fetch-status-info-id* (reagent/atom nil))

(defn fetch-status-info []
  ;(reset! status-info-data* nil)
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :status)
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Status-Info"
                               :handler-key :auth
                               :retry-fn #'fetch-status-info}
                              :chan resp-chan)]
    (reset! fetch-status-info-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= :status (-> @routing/state* :handler-key))
                     (or (= (:status resp) 200)
                         (>= 900(:status resp)))
                     (= id @fetch-status-info-id*))
            (reset! status-info-data* (:body resp))
            (js/setTimeout  #(fetch-status-info) 1000))))))

(defn info-page []
  [:div.status
   [routing/hidden-state-component
    {:did-mount fetch-status-info}]
   [breadcrumbs/nav-component @breadcrumbs/left* []]
   [:h1 "Leihs-Admin Server-Status Info"]
   [:p "The data shown below is mostly of interest for monitoring or debugging."]
   (when-let [status-info-data @status-info-data*]
     [:pre.bg-light
      (with-out-str (pprint status-info-data))])])

