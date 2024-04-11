(ns leihs.admin.resources.buildings.shared
  (:require [leihs.admin.constants :as defaults]
            [leihs.core.constants :refer [GENERAL_BUILDING_UUID]]))

(def default-query-params
  {:page 1
   :per-page defaults/PER-PAGE
   :term nil})

(defn normalized-query-parameters [query-params]
  (merge default-query-params query-params))

#?(:clj (def is-general-select-expr
          [[:raw (format "CASE WHEN buildings.id = '%s' THEN TRUE ELSE FALSE END"
                         GENERAL_BUILDING_UUID)]
           :is_general]))
