(ns leihs.procurement.resources.template
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.data :refer [diff]]

                [taoensso.timbre :refer [debug info warn error spy]]

            [leihs.core.core :refer [raise]]
            [leihs.procurement.utils.sql :as sql]
            [taoensso.timbre :refer [debug info warn error]]))

(def ALLOWED-KEYS-FOR-USED-TEMPLATE #{:is_archived})

(def templates-base-query
  (-> (sql/select :procurement_templates.*)
      (sql/from :procurement_templates)))

(defn get-template-by-id
  [tx id]
  (println ">o> tocheck ??? get-template-by-id" id)

  (spy (-> templates-base-query
           (sql/merge-where [:= :procurement_templates.id id])
           sql/format
           (->> (jdbc/query tx))
           first)))

(defn insert-template!
  [tx tmpl]
  (spy (jdbc/execute! tx
                      (-> (sql/insert-into :procurement_templates)
                          (sql/values [tmpl])
                          sql/format))))

(defn validate-update-attributes [tx tmpl]
  (let [req-exist? (spy (-> (sql/select :%count.*)
                            (sql/from :procurement_requests)
                            (sql/where [:= :template_id (:id (spy tmpl))])
                            sql/format
                            (->> (jdbc/query tx))
                            first :count (> 0)))]
    (if (spy req-exist?)
      (select-keys tmpl ALLOWED-KEYS-FOR-USED-TEMPLATE))
    tmpl))

(defn update-template!
  [tx tmpl]
  (spy (jdbc/execute! tx (-> (sql/update :procurement_templates)
                        (sql/sset (validate-update-attributes tx tmpl))
                        (sql/where [:= :procurement_templates.id (:id tmpl)])
                        sql/format))))

(defn delete-template!
  [tx id]
  (spy (jdbc/execute! tx (-> (sql/delete-from :procurement_templates)
                        (sql/where [:= :procurement_templates.id id])
                        sql/format))))

(defn get-template
  ([context _ value]
   (println ">o> tocheck ??? get-template" value)

   (spy (get-template-by-id (-> context
                           :request
                           :tx)
                       (or (:value value)                   ; for RequestFieldTemplate
                           (:template_id value)))))
  ([tx tmpl]
   (spy (let [where-clause (sql/map->where-clause :procurement_templates tmpl)]
     (-> templates-base-query
         (sql/merge-where where-clause)
         sql/format
         (->> (jdbc/query tx))
         first)))))

(defn requests-count [{{:keys [tx]} :request} _ {:keys [id]}]
  (spy (-> (sql/select :%count.*)
      (sql/from :procurement_requests)
      (sql/where [:= :template_id id])
      sql/format
      (->> (jdbc/query tx))
      first
      :count)))
