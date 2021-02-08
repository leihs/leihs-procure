(ns leihs.admin.resources.groups.shared
  (:require [leihs.admin.defaults :as defaults]))

(def default-fields
  #{
    :name
    :id
    :organization
    :org_id
    })

(def available-fields
  #{
    :admin_protected
    :created_at
    :description
    :id
    :name
    :org_id
    :organization
    :system_admin_protected
    })

(def default-query-params
  {:page 1
   :per-page defaults/PER-PAGE
   :org-member nil
   :term nil
   :including-user nil})

(defn normalized-query-parameters [query-params]
  (merge default-query-params query-params))
