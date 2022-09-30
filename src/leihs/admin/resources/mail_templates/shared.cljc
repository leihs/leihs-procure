(ns leihs.admin.resources.mail-templates.shared
  (:require [leihs.admin.constants :as defaults]
            [leihs.core.constants :refer [GENERAL_BUILDING_UUID]]))

(def default-fields
  #{:id
    :name
    :code})

(def default-query-params
  {:page 1
   :per-page defaults/PER-PAGE
   :term nil})

(defn normalized-query-parameters [query-params]
  (merge default-query-params query-params))
