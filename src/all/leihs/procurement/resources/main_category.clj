(ns leihs.procurement.resources.main-category
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.utils.ds :as ds]
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
  (-> value
      :main_category_id
      main-category-query-by-id
      (->> (jdbc/query (-> context :requext :tx)))
      first))

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

(defn can-delete? [context _ value]
  (-> (jdbc/query
        (-> context :request :tx)
        (-> (sql/call
              :and
              (sql/call
                :not
                (sql/call
                  :exists
                  (-> (sql/select true)
                      (sql/from [:procurement_requests :pr])
                      (sql/merge-join [:procurement_categories :pc]
                                      [:= :pc.id :pr.category_id])
                      (sql/merge-where [:= :pc.main_category_id (:id value)]))))
              (sql/call
                :not
                (sql/call
                  :exists
                  (-> (sql/select true)
                      (sql/from [:procurement_templates :pt])
                      (sql/merge-join [:procurement_categories :pc]
                                      [:= :pc.id :pt.category_id])
                      (sql/merge-where [:= :pc.main_category_id (:id value)]))))) 
            (vector :result)
            sql/select
            sql/format
            ))
      first
      :result))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
