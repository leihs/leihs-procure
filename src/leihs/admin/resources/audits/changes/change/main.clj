(ns leihs.admin.resources.audits.changes.change.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.tools.logging :as logging]
    [compojure.core :as cpj]
    [leihs.admin.paths :refer [path]]
    [leihs.core.sql :as sql]
    [logbug.catcher :as catcher]
    [logbug.debug :as debug]
    ))

(defn get-change
  [{{id :audited-change-id} :route-params
    tx :tx :as request}]
  {:body (->> (-> (sql/select :audited_changes.*)
                  (sql/from :audited_changes)
                  (sql/merge-where [:= :id id])
                  sql/format)
              (jdbc/query tx) first )})

(defn routes [request]
  (case (:handler-key request)
    :audited-change (get-change request)))

;#### debug ###################################################################

;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/wrap-with-log-debug #'users-formated-query)
