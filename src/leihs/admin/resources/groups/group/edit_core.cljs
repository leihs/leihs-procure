(ns leihs.admin.resources.groups.group.edit-core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [timeout]]
   [cljs.pprint :refer [pprint]]

   [leihs.admin.common.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.form-components :as form-components]
   [leihs.admin.common.users-and-groups.core :as users-and-groups]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.groups.group.core :as group]
   [leihs.admin.state :as state]

   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [leihs.core.user.front :as current-user]
   [reagent.core :as reagent]))

(defn inner-form-component []
  [:div
   [:div.form-row
    [:div.col-md [form-components/input-component group/data* [:name]
                  :label "Name"]]]

   [users-and-groups/protect-form-fiels-row-component group/data*]
   [users-and-groups/org-form-fields-row-component group/data*]

   [:div.form-row
    [:div.col-md
     [form-components/input-component group/data* [:description]
      :element :textarea
      :label "Description"]]]])

