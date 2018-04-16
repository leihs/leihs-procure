(ns leihs.procurement.resources.organizations
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as logging]
            [leihs.procurement.utils.sql :as sql]
            [logbug.debug :as debug]
            ))

(def organizations-base-query
  (-> (sql/select :procurement_organizations.*)
      (sql/from :procurement_organizations)))

(defn organizations-query [_ args value]
  (let [root-only (:root_only args)
        id (:id value)]
    (sql/format
      (cond-> organizations-base-query
        root-only
        (sql/merge-where [:=
                          :procurement_organizations.parent_id
                          nil])
        id
        (sql/merge-where [:=
                          :procurement_organizations.parent_id
                          id])))))

(defn get-organizations [context args value]
  (jdbc/query (-> context :request :tx)
              (organizations-query context args value)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
