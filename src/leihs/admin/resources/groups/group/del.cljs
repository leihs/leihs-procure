(ns leihs.admin.resources.groups.group.del
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.admin.resources.groups.group.core :refer [group-id* data* debug-component clean-and-fetch fetch-group group-name-component group-id-component]]
    [leihs.admin.common.icons :as icons]

    [leihs.admin.common.components :as components]
    [leihs.admin.common.form-components :as form-components]
    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.groups.group.breadcrumbs :as breadcrumbs]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :refer [wait-component]]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))



;;; delete ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-group [& args]
  (go (when (some->
              {:chan (async/chan)
               :url (path :group (-> @routing/state* :route-params))
               :method :delete}
              http-client/request :chan <!
              http-client/filter-success!)
        (accountant/navigate!
          (path :groups {})))))


(defn delete-form-component []
  [:form.form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (delete-group))}
   [form-components/delete-submit-component]])

(defn page []
  [:div.group-delete
   [breadcrumbs/nav-component
    (conj  @breadcrumbs/left* [breadcrumbs/delete-li]) []]
   [:h1 "Delete Group "
    [group-name-component]]
   [group-id-component]
   [delete-form-component]])
