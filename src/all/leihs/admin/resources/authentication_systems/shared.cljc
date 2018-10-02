(ns leihs.admin.resources.authentication-systems.shared)

(def default-fields
  #{
    :enabled
    :id
    :name
    :priority
    :type
    })

(def available-fields
  #{
    :created_at
    :description
    :enabled
    :id
    :name
    :priority
    :type
    })

(def default-query-parameters {:page 1 :per-page 12 :type :any :term nil})

(defn normalized-query-parameters [query-params]
  (merge default-query-parameters query-params))
