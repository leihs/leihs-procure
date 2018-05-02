(ns leihs.procurement.resources.main-category
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as logging]
            [leihs.procurement.utils.sql :as sql]
            [logbug.debug :as debug]
            ))

(def main-category-base-query
 (-> (sql/select :procurement_main_categories.*)
     (sql/from :procurement_main_categories))) 

(defn main-category-query-by-id [id]
  (-> main-category-base-query
      (sql/where [:= :procurement_main_categories.id id])
      sql/format))

(defn main-category-query-by-name [mc-name]
  (-> main-category-base-query
      (sql/where [:= :procurement_main_categories.name mc-name])
      sql/format))

(defn get-main-category [context _ value]
  (first (jdbc/query (-> context :request :tx)
                     (main-category-query-by-id (:main_category_id value)))))

(defn get-main-category-by-name [tx mc-name]
  (first (jdbc/query tx (main-category-query-by-name mc-name))))

(defn insert-main-category! [tx mc]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_main_categories)
                     (sql/values [mc])
                     sql/format)))

(defn update-main-category! [tx mc]
  (jdbc/execute! tx
                 (-> (sql/update :procurement_main_categories)
                     (sql/sset mc)
                     (sql/where [:= :procurement_main_categories.id (:id mc)])
                     sql/format
                     )))

;#### debug ###################################################################
(logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
