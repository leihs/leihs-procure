(ns leihs.admin.resources.system.authentication-systems.shared
  (:require
   [leihs.admin.constants :as defaults]))

(def default-fields
  #{:enabled
    :id
    :name
    :priority
    :type})

(def available-fields
  #{:created_at
    :description
    :enabled
    :id
    :name
    :priority
    :type})

(def default-query-params {:page 1 :per-page defaults/PER-PAGE :type :any :term nil})

(defn normalized-query-parameters [query-params]
  (merge default-query-params query-params))


