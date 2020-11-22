(ns leihs.admin.resources.system.system-admins.shared
  (:require
    [leihs.admin.resources.users.shared]
    ))

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

(def default-query-params
  (merge leihs.admin.resources.users.shared/default-query-params
         {:is-system-admin "yes"}))

(defn normalized-query-parameters [query-params]
  (merge default-query-params query-params))

