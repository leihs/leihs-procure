(ns leihs.admin.resources.audits.changes.shared
  (:require
   [leihs.admin.constants :as defaults]))

(def default-query-params
  {:page 1
   :pkey ""
   :table ""
   :term ""
   :tg-op ""
   :per-page defaults/PER-PAGE})

(defn normalized-query-parameters [query-params]
  (merge default-query-params query-params))
