(ns leihs.procurement.resources.template
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(def templates-base-query
  (-> (sql/select :procurement_templates.*)
      (sql/from :procurement_templates)))

(defn insert-template!
  [tx tmpl]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_templates)
                     (sql/values [tmpl])
                     sql/format)))

(defn update-template!
  [tx tmpl]
  (jdbc/execute! tx
                 (-> (sql/update :procurement_templates)
                     (sql/sset tmpl)
                     (sql/where [:= :procurement_templates.id (:id tmpl)])
                     sql/format)))

(defn delete-template!
  [tx id]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_templates)
                     (sql/where [:= :procurement_templates.id id])
                     sql/format)))

(defn get-template-by-id
  [tx id]
  (-> templates-base-query
      (sql/merge-where [:= :procurement_templates.id id])
      sql/format
      (->> (jdbc/query tx))
      first))

(defn get-template
  ([context _ value]
   (get-template-by-id (-> context
                           :request
                           :tx)
                       (or (:value value) ; for RequestFieldTemplate
                           (:template_id value))))
  ([tx tmpl]
   (let [where-clause (sql/map->where-clause :procurement_templates tmpl)]
     (-> templates-base-query
         (sql/merge-where where-clause)
         sql/format
         (->> (jdbc/query tx))
         first))))

(defn can-delete?
  [context _ value]
  (-> (sql/call
        :not
        (sql/call :exists
                  (-> (sql/select true)
                      (sql/from [:procurement_requests :pr])
                      (sql/merge-where [:= :pr.template_id (:id value)]))))
      (vector :result)
      sql/select
      sql/format
      (->> (jdbc/query (-> context
                           :request
                           :tx)))
      first
      :result))
