(ns leihs.admin.resources.system-admins.groups.shared
  (:require
    [leihs.core.json :as json]
    ))

(defn filter-value [query-params]
  (if-not (contains? query-params :system-admin-groups)
    false
    (try
      (-> query-params :system-admin-groups json/from-json)
      (catch #?(:clj Exception
                :cljs js/Object) _
        false))))

