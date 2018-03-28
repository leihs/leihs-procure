(ns leihs.admin.resources.delegation.users.shared
  (:require
    [leihs.admin.utils.json :as json]
    ))

(defn delegation-users-filter-value [query-params]
  (if-not (contains? query-params :delegation-users-only) 
    true
    (try 
      (-> query-params :delegation-users-only json/from-json)
      (catch #?(:clj Exception
                :cljs js/Object) _
        true))))
