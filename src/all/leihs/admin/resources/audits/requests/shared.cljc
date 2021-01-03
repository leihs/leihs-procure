(ns leihs.admin.resources.audits.requests.shared
  (:require
    [leihs.admin.defaults :as defaults]))

(def default-query-params
  {:user-uid ""
   :page 1
   :method ""
   :per-page defaults/PER-PAGE})


(defn normalized-query-parameters [query-params]
  (merge default-query-params query-params))
