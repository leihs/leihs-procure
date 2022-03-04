(ns leihs.admin.resources.groups.group.edit
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]

    [leihs.admin.common.form-components :refer [checkbox-component input-component save-submit-component]]
    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.groups.group.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.groups.group.core :refer [group-id* data* debug-component clean-and-fetch fetch-group group-name-component group-id-component]]
    [leihs.admin.resources.groups.group.edit-core :as edit-core]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :refer [wait-component]]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(defn patch [& args]
  (go (when (some->
              {:chan (async/chan)
               :url (path :group {:group-id @group-id*})
               :method :patch
               :json-params @data*}
              http-client/request
              :chan <! http-client/filter-success!)
        (accountant/navigate!
          (path :group {:group-id @group-id*})))))

(defn edit-form-component []
  [:form.form
   {:auto-complete :off
    :on-submit (fn [e]
                 (.preventDefault e)
                 (patch))}
   [edit-core/inner-form-component]
   [save-submit-component]])

(defn page []
  [:div.edit-group
   [routing/hidden-state-component
    {:did-mount clean-and-fetch
     :did-change clean-and-fetch}]
   [breadcrumbs/nav-component (conj @breadcrumbs/left* [breadcrumbs/edit-li]) []]
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Edit Group "]
      [group-name-component]]]]
   [edit-form-component]
   [debug-component]])
