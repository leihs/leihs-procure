(ns leihs.admin.resources.audits.requests.shared
  (:require
   [leihs.admin.constants :as defaults]))

(def default-query-params
  {:user-uid ""
   :page 1
   :method ""
   :per-page defaults/PER-PAGE})
