(ns leihs.admin.resources.group.users.shared
  (:require
    [leihs.admin.utils.json :as json]
    ))

(defn group-users-filter-value [query-params]
  (if-not (contains? query-params :group-users-only) 
    true
    (try 
      (-> query-params :group-users-only json/from-json)
      (catch #?(:clj Exception
                :cljs js/Object) _
        true))))
