(ns leihs.procurement.resources.category
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(def category-base-query
  (-> (sql/select :procurement_categories.*)
      (sql/from :procurement_categories)))

(defn category-query
  [id]
  (-> category-base-query
      (sql/where [:= :procurement_categories.id id])
      sql/format))

(defn get-category
  ([context _ value]
   (first (jdbc/query (-> context
                          :request
                          :tx)
                      (category-query (or (:value value) ; for
                                          ; RequestFieldCategory
                                          (:category_id value))))))
  ([tx catmap]
   (let [where-clause (sql/map->where-clause :procurement_categories catmap)]
     (first (jdbc/query tx
                        (-> category-base-query
                            (sql/merge-where where-clause)
                            sql/format))))))

(defn get-category-by-id [tx id] (first (jdbc/query tx (category-query id))))

(defn can-delete?
  [context _ value]
  (-> (jdbc/query
        (-> context
            :request
            :tx)
        (-> (sql/call
              :and
              (sql/call :not
                        (sql/call :exists
                                  (-> (sql/select true)
                                      (sql/from [:procurement_requests :pr])
                                      (sql/merge-where [:= :pr.category_id
                                                        (:id value)]))))
              (sql/call :not
                        (sql/call :exists
                                  (-> (sql/select true)
                                      (sql/from [:procurement_templates :pt])
                                      (sql/merge-where [:= :pt.category_id
                                                        (:id value)])))))
            (vector :result)
            sql/select
            sql/format))
      first
      :result))

(defn update-category!
  [tx c]
  (jdbc/execute! tx
                 (-> (sql/update :procurement_categories)
                     (sql/sset c)
                     (sql/where [:= :procurement_categories.id (:id c)])
                     sql/format)))

(defn insert-category!
  [tx c]
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
