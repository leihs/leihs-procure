(ns leihs.admin.resources.system.authentication-systems.authentication-system.users.shared
  (:require
    [leihs.core.json :as json]
    ))

(defn authentication-system-users-filter-value [query-params]
  (if-not (contains? query-params :authentication-system-users-only)
    true
    (try
      (-> query-params :authentication-system-users-only json/from-json)
      (catch #?(:clj Exception
                :cljs js/Object) _
        true))))
