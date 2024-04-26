(ns leihs.admin.common.membership.users.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [honey.sql.helpers :as sql]
   [leihs.admin.common.membership.users.shared :refer [DEFAULT-QUERY-PARAMS
                                                       MEMBERSHIP-QUERY-PARAM-KEY]]
   [leihs.core.core :refer [presence str]]))

(defn filter-by-membership
  [query member-expr direct-member-expr group-expr request]
  (case (-> (merge
             DEFAULT-QUERY-PARAMS
             (:query-params request))
            MEMBERSHIP-QUERY-PARAM-KEY presence str)
    ("" "member") (sql/where query member-expr)
    "any" query
    "direct" (sql/where query direct-member-expr)
    "group" (sql/where query group-expr)
    "non" (sql/where query [:not member-expr])))

(defn extend-with-membership
  [query member-expr direct-member-expr group-expr request]
  (-> query
      (sql/select [[:case
                    member-expr true
                    :else false] :member])
      (sql/select [[:case
                    direct-member-expr true
                    :else false] :direct_member])
      (sql/select [[:case
                    group-expr true
                    :else false] :group_member])
      (filter-by-membership member-expr direct-member-expr group-expr request)))
