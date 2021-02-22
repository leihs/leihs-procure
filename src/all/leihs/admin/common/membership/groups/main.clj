(ns leihs.admin.common.membership.groups.main
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.sql :as sql]

    [leihs.admin.paths :refer [path]]
    [leihs.admin.common.membership.groups.shared :as shared]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]

    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]))



(defn filter-by-membership
  [query member-expr request]
  (case (-> (merge
              shared/default-query-params
              (:query-params request))
            #_logging/spy
            :membership presence str)
    ( "" "member") (sql/merge-where query member-expr)
    "any" query
    "non" (sql/merge-where query [:not member-expr])))

(defn extend-with-membership [query member-expr request]
  (-> query
      (sql/merge-select [(sql/call :case
                                   member-expr true
                                   :else false) :member])
      (filter-by-membership member-expr request)))



;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/wrap-with-log-debug #'filter-suspended)
;(debug/wrap-with-log-debug #'groups-formated-query)
;(debug/debug-ns *ns*)
