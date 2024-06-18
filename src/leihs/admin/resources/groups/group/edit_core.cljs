(ns leihs.admin.resources.groups.group.edit-core
  (:require [leihs.admin.common.form-components :as form-components]
            [leihs.admin.common.users-and-groups.core :as users-and-groups]
            [leihs.admin.resources.groups.group.core :as group]))

(defn inner-form-component [data*]
  [:div
   [:div.form-row
    [:div.col-md [form-components/input-component data* [:name]
                  :label "Name"]]]

   [users-and-groups/protect-form-fiels-row-component data*]
   [users-and-groups/org-form-fields-row-component data*]

   [:div.form-row
    [:div.col-md
     [form-components/input-component data* [:description]
      :element :textarea
      :label "Description"]]]])
