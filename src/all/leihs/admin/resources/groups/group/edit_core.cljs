(ns leihs.admin.resources.groups.group.edit-core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as current-user]

    [leihs.admin.resources.groups.group.core :as group]
    [leihs.admin.common.breadcrumbs :as breadcrumbs]
    [leihs.admin.common.form-components :as form-components]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(defn inner-form-component []
  [:div
   [:div.form-row
    [:div.col-md-6 [form-components/input-component group/data* [:name]
                    :label "Name"]]
    [:div.col-md-2 [form-components/input-component group/data* [:org_id]
                    :label "Org ID"]]
    [:div.col-md-4
     [form-components/checkbox-component group/data* [:protected]
      :disabled (not @current-user/admin?*)
      :label "Admin protected"
      :hint [:span "A protected Group can only be modified by admins and in particular not by inventory-pool staff. "
             "This is often set for accounts which are automatically managed via the API. "  ]]]]
   [:div.form-row
    [:div.col-md
     [form-components/input-component group/data* [:description]
      :element :textarea
      :label "Description"]]]])

