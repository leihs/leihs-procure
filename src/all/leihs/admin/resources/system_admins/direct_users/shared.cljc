(ns leihs.admin.resources.system-admins.direct-users.shared
  (:require
    [leihs.core.json :as json]
    ))

(defn filter-value [query-params]
  (if-not (contains? query-params :system-admin-direct-users)
    false
    (try
      (-> query-params :system-admin-direct-users json/from-json)
      (catch #?(:clj Exception
                :cljs js/Object) _
        false))))

