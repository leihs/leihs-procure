(ns leihs.admin.common.membership.users.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.java.jdbc :as jdbc]
   [compojure.core :as cpj]
   [leihs.admin.common.membership.users.shared :refer [DEFAULT-QUERY-PARAMS MEMBERSHIP-QUERY-PARAM-KEY]]
   [leihs.admin.paths :refer [path]]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.sql :as sql]
   [logbug.debug :as debug]))

(defn filter-by-membership
  [query member-expr direct-member-expr group-expr request]
  (case (-> (merge
             DEFAULT-QUERY-PARAMS
             (:query-params request))
            MEMBERSHIP-QUERY-PARAM-KEY presence str)
    ("" "member") (sql/merge-where query member-expr)
    "any" query
    "direct" (sql/merge-where query direct-member-expr)
    "group" (sql/merge-where query group-expr)
    "non" (sql/merge-where query [:not member-expr])))

(defn extend-with-membership
  [query member-expr direct-member-expr group-expr request]
  (-> query
      (sql/merge-select [(sql/call :case
                                   member-expr true
                                   :else false) :member])
      (sql/merge-select [(sql/call :case
                                   direct-member-expr true
                                   :else false) :direct_member])
      (sql/merge-select [(sql/call :case
                                   group-expr true
                                   :else false) :group_member])
      (filter-by-membership member-expr direct-member-expr group-expr request)))
