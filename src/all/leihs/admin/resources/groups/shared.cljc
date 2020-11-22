(ns leihs.admin.resources.groups.shared
  (:require [leihs.admin.defaults :as defaults]))
(def default-fields
  #{
    :name
    :id
    :org_id
    :protected
    })

(def available-fields
  #{
    :created_at
    :description
    :id
    :name
    :org_id
    })

(def default-query-params
  {:page 1
   :per-page defaults/PER-PAGE
   :org-member nil
   :term nil
   :including-user nil})

(defn normalized-query-parameters [query-params]
  (merge default-query-params query-params))
