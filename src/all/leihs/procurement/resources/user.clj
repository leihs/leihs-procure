(ns leihs.procurement.resources.user
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as logging]
            [leihs.procurement.utils.sql :as sql]
            [leihs.procurement.utils.ds :refer [get-ds]]
            [logbug.debug :as debug]))

(def user-base-query
  (-> (sql/select :users.*)
      (sql/from :users)))

(defn get-user
  [context _ value]
  (first (jdbc/query (-> context
                         :request
                         :tx)
                     (-> user-base-query
                         (sql/where [:= :users.id (:user_id value)])
                         sql/format))))

(defn get-user-by-id
  [tx id]
  (first (jdbc/query tx
                     (-> user-base-query
                         (sql/where [:= :users.id id])
                         sql/format))))

(defn procurement-inspector?
  [tx user]
  (:result
    (first (jdbc/query
             tx
             (-> (sql/select
                   [(sql/call :exists
                              (-> (sql/select 1)
                                  (sql/from :procurement_category_inspectors)
                                  (sql/where
                                    [:= :procurement_category_inspectors.user_id
                                     (:id user)]))) :result])
                 sql/format)))))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
