(ns leihs.admin.resources.inventory-pools.shared
  (:require
   [leihs.admin.constants :as defaults]
   [leihs.admin.paths :refer [path]]))

(def default-fields
  #{:is_active
    :id
    :name
    :shortname})

(def default-query-params
  {:page 1
   :per-page defaults/PER-PAGE
   :active ""
   :order [["name" "asc"] ["id" "asc"]]
   :term nil})

(def inventory-pool-path (path :inventory-pool {:inventory-pool-id ":inventory-pool-id"}))

(defn normalized-query-parameters [query-params]
  (merge default-query-params query-params))
