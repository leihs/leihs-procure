(ns leihs.admin.resources.system.database.audits.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.anti-csrf.front :as anti-csrf]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.icons :as icons]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.shared :refer [humanize-datetime-component gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.system.breadcrumbs :as system-breadcrumbs]
    [leihs.admin.resources.system.database.breadcrumbs :as database-breadcrumbs]
    [leihs.admin.utils.seq :refer [with-index]]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.moment]
    [reagent.core :as reagent]
    ))


(def before-date* (reagent/atom  (-> (js/moment)
                                     (.subtract 1 "year")
                                     (.format "YYYY-MM-DD"))))

(defn before-form []
  [:form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (accountant/navigate!
                   (path :database-audits-before
                         {:before-date @before-date*})))}
   [:div.form-group
    [:label {:for :before} "Date"]
    [:input.form-control
     {:type :date
      :value @before-date*
      :on-change (fn [e]
                   (reset! before-date* (-> e .-target .-value)))}]]
   [:div.form-group.float-right
    [:button.btn.btn-primary
     {:type :submitt}
     ;[:i.fa.fas.fa-download]
     [:span " Continue "]]]])

(defn index-page []
  [:div.database.audits
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (system-breadcrumbs/system-li)
      (database-breadcrumbs/database-li)
      (database-breadcrumbs/database-audits-li)
      ]
     [])
   [:h1 "Audits"]
   [:h2 "Clean-Up"]
   [:p "Download and delete audits before: "]
   [before-form]
   ])


;;; action ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn download-form []
  [:form.form
   {:method :post}
   [anti-csrf/hidden-form-group-token-component]
   [:div.form-group.float-right
    [:button.btn.btn-primary
     {:type :submitt}
     [:i.fa.fas.fa-download]
     [:span " Download "]]]
   [:div.clearfix]])

(defn section-download []
  [:section
   [:h2 "Download"]
   [:p [:b "API notes:"] " the download via a web-browser uses " [:code " HTTP POST" ] ". "
    "This is a result of considering the possible size of the data and some technical restrictions imposed by browsers. "
    "Via the API " [:b [:code " HTTP GET "]] " with the proper accept header " [:b [:code "application/json"]] " should be used. "]
   [download-form]])

(defn do-delete []
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (-> @routing/state* :url)
                               :method :delete}
                              {:modal true
                               :title "Delete Audits"}
                              :chan resp-chan)]

    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200))
            (accountant/navigate! (path :database-audits)))))))

(defn delete-form []
  [:form.form
   {:on-submit (fn [e]
                  (.preventDefault e)
                  (do-delete))}
   [anti-csrf/hidden-form-group-token-component]
   [:div.form-group.float-right
    [:button.btn.btn-danger
     {:type :submitt}
     icons/delete [:span " Delete "]]]
   [:div.clearfix]])

(defn section-delete []
  [:section
   [:h2 "Delete"]
   [:p.text-danger
    "If archiving audits is a requirement make sure the download has completed and that the data is stored in a safe place
    before deleting it."]
   [delete-form]])

(defn before-page []
  [:div.database.audits-before
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (system-breadcrumbs/system-li)
      (database-breadcrumbs/database-li)
      (database-breadcrumbs/database-audits-li)
      (database-breadcrumbs/database-audits-before-li (-> @routing/state* :route-params :before-date))][])
   [:h1 "Audits Before " (-> @routing/state* :route-params :before-date)]
   [section-download]
   [section-delete]])
