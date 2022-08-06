(ns leihs.admin.resources.audits.changes.change.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [compojure.core :as cpj]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [leihs.admin.paths :refer [path]]
    [logbug.debug :as debug]
    [next.jdbc :as jdbc]
    [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
    [taoensso.timbre :refer [error warn info debug spy]]
    ))

(defn get-change
  [{{id :audited-change-id} :route-params
    tx-next :tx-next :as request}]
  {:body (-> (sql/select :audited_changes.*)
             (sql/from :audited_changes)
             (sql/where [:= :id id])
             sql-format
             (->> (jdbc/execute-one! tx-next)))})

(defn routes [request]
  (case (:handler-key request)
    :audited-change (get-change request)))

;#### debug ###################################################################

;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/wrap-with-log-debug #'users-formated-query)
