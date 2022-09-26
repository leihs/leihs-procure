(ns leihs.admin.resources.rooms.shared
  (:require [leihs.admin.constants :as defaults]))

(def default-fields
  #{:id
    :name
    :description
    :building_id})

(def default-query-params
  {:page 1
   :per-page defaults/PER-PAGE
   :term nil})

(defn normalized-query-parameters [query-params]
  (merge default-query-params query-params))
