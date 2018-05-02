(ns leihs.procurement.resources.category
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.utils.ds :as ds]
            [leihs.procurement.utils.sql :as sql]
            [logbug.debug :as debug]  
            ))

(def category-base-query
  (-> (sql/select :procurement_categories.*)
      (sql/from :procurement_categories)))

(defn category-query [id]
  (-> category-base-query 
      (sql/where [:= :procurement_categories.id id])
      sql/format))

(defn get-category
  ([context _ value]
   (first (jdbc/query (-> context :request :tx)
                      (category-query (:category_id value)))))
  ([tx catmap]
   (let [where-clause
         (sql/map->where-clause :procurement_categories catmap)]
     (first (jdbc/query tx (-> category-base-query
                               (sql/merge-where where-clause)
                               sql/format
                               ))))))

(defn get-category-by-id [tx id]
  (first (jdbc/query tx (category-query id))))

(defn inspectable-by? [tx user category]
  (:result
    (jdbc/query
      tx
      (-> (sql/select
            [(sql/call
               :exists
               (-> (sql/select 1)
                   (sql/from :procurement_category_inspectors)
                   (sql/where [:=
                               :procurement_category_inspectors.user_id
                               (:id user)])
                   (sql/merge-where [:=
                                     :procurement_category_inspectors.category_id
                                     (:id category)])))
             :result])
          sql/format))))

(defn update-category! [tx c]
  (jdbc/execute! tx
                 (-> (sql/update :procurement_categories)
                     (sql/sset c)
                     (sql/where [:= :procurement_categories.id (:id c)])
                     sql/format)))

(defn insert-category! [tx c]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_categories)
                     (sql/values [c])
                     sql/format)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
