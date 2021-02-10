(ns leihs.admin.resources.groups.group.create
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]

    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.resources.groups.group.edit-core :as edit-core]
    [leihs.admin.resources.groups.group.core :refer [group-id* data* debug-component clean-and-fetch fetch-group group-name-component group-id-component]]
    [leihs.admin.resources.groups.breadcrumbs :as breadcrumbs]
    [leihs.admin.common.form-components :refer [checkbox-component input-component create-submit-component]]
    [leihs.admin.utils.misc :refer [wait-component]]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(defn post [& args]
  (go (when-let [body (some->
                        {:chan (async/chan)
                         :url (path :groups)
                         :method :post
                         :json-params @data*}
                        http-client/request
                        :chan <! http-client/filter-success!
                        :body)]
        (accountant/navigate!
          (path :group {:group-id (:id body)})))))

(defn clean-and-preset []
  (reset! data* {}))

(defn create-form-component []
  [:form.form
   {:auto-complete :off
    :on-submit (fn [e]
                 (.preventDefault e)
                 (post))}
   [edit-core/inner-form-component]
   [create-submit-component]])

(defn page []
  [:div.edit-group
   [routing/hidden-state-component
    {:did-mount clean-and-preset}]
   [breadcrumbs/nav-component
    (conj @breadcrumbs/left* [breadcrumbs/create-li])]
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Create Group "]]]]
   [create-form-component]
   [debug-component]])
