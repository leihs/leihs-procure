(ns leihs.admin.common.membership.groups.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [honey.sql.helpers :as sql]
   [leihs.admin.common.membership.groups.shared :as shared]
   [leihs.core.core :refer [presence str]]))

(defn filter-by-membership
  [query member-expr request]
  (case (-> (merge
             shared/default-query-params
             (:query-params request))
            :membership presence str)
    ("" "member") (sql/where query member-expr)
    "any" query
    "non" (sql/where query [:not member-expr])))

(defn extend-with-membership [query member-expr request]
  (-> query
      (sql/select [[:case
                    member-expr true
                    :else false] :member])
      (filter-by-membership member-expr request)))

;#### debug ###################################################################
;(debug/wrap-with-log-debug #'filter-suspended)
;(debug/wrap-with-log-debug #'groups-formated-query)
;(debug/debug-ns *ns*)
