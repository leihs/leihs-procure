(ns leihs.admin.resources.groups.shared)

(def default-fields 
  #{
    :name
    :id 
    :org_id
    })

(def available-fields
  #{
    :created_at
    :description
    :id 
    :name
    :org_id
    })

(def default-query-parameters {:page 1 :per-page 12 :type :any :term nil})

(defn normalized-query-parameters [query-params]
  (merge default-query-parameters query-params))
