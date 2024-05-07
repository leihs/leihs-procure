(ns leihs.admin.resources.audits.changes.change.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.uuid :refer [uuid]]
   [next.jdbc :as jdbc]))

(defn get-change
  [{{id :audited-change-id} :route-params
    tx :tx :as request}]
  {:body (-> (sql/select :audited_changes.*)
             (sql/from :audited_changes)
             (sql/where [:= :id (uuid id)])
             sql-format
             (->> (jdbc/execute-one! tx)))})

(defn routes [request]
  (case (:handler-key request)
    :audited-change (get-change request)))

;#### debug ###################################################################

;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/wrap-with-log-debug #'users-formated-query)
