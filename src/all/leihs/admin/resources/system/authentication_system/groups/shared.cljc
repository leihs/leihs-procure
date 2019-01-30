(ns leihs.admin.resources.system.authentication-system.groups.shared
  (:require
    [leihs.core.json :as json]
    ))

(defn filter-value [query-params]
  (if-not (contains? query-params :authentication-system-groups)
    false
    (try
      (-> query-params :authentication-system-groups json/from-json)
      (catch #?(:clj Exception
                :cljs js/Object) _
        false))))

