(ns leihs.admin.shared.membership.users.back
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.sql :as sql]

    [leihs.admin.paths :refer [path]]
    [leihs.admin.shared.membership.users.shared :refer [default-query-params]]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]

    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]))

(defn filter-by-membership
  [query member-expr direct-member-expr group-expr request]
  (case (-> (merge
              default-query-params
              (:query-params request))
            :membership presence str)
    ("" "member") (sql/merge-where query member-expr)
    "any" query
    "direct" (sql/merge-where query member-expr)
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
