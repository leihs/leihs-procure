(ns leihs.admin.resources.groups.group.edit-core
  (:require [leihs.admin.common.form-components :as form-components]
            [leihs.admin.common.users-and-groups.core :as users-and-groups]
            [leihs.admin.resources.groups.group.core :as group]))

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
