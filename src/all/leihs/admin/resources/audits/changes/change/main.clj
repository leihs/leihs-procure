(ns leihs.admin.resources.audits.changes.change.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.core.sql :as sql]
    [leihs.admin.paths :refer [path]]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [logbug.catcher :as catcher]
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
;(logging-config/set-logger! :level :debug)
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/wrap-with-log-debug #'users-formated-query)
