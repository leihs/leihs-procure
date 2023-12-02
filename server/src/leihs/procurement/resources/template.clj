(ns leihs.procurement.resources.template
  (:require

    ;[clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sqlp]

          [honey.sql :refer [format] :rename {format sql-format}]
          [leihs.core.db :as db]
          [next.jdbc :as jdbc]
          [honey.sql.helpers :as sql]

            [clojure.data :refer [diff]]
            [leihs.core.core :refer [raise]]
            [taoensso.timbre :refer [debug info warn error]]))

(def ALLOWED-KEYS-FOR-USED-TEMPLATE #{:is_archived})

(def templates-base-query
  (-> (sql/select :procurement_templates.*)
      (sql/from :procurement_templates)))

(defn get-template-by-id
  [tx id]
  (-> templates-base-query
      (sql/where [:= :procurement_templates.id [:cast id :uuid]])
      sql-format
      (->> (jdbc/execute! tx))
      ))

(defn insert-template!
  [tx tmpl]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_templates)
                     (sql/values [tmpl])
                     sql-format)))

(defn validate-update-attributes [tx tmpl]
  (let [req-exist? (-> (sql/select :%count.*)
                       (sql/from :procurement_requests)
                       (sql/where [:= :template_id [:cast(:id tmpl):uuid]])
                       sql-format
                       (->> (jdbc/execute-one! tx))
                       :count (> 0) )]
    (if req-exist?
      (select-keys tmpl ALLOWED-KEYS-FOR-USED-TEMPLATE))
      tmpl))

(defn update-template!
  [tx tmpl]
  (jdbc/execute! tx
                 (-> (sql/update :procurement_templates)
                     (sql/set (validate-update-attributes tx tmpl))
                     (sql/where [:= :procurement_templates.id [:cast (:id tmpl) :uuid]])
                     sql-format)))

(defn delete-template!
  [tx id]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_templates)
                     (sql/where [:= :procurement_templates.id [:cast id :uuid]])
                     sql-format)))

(defn get-template
  ([context _ value]
   (get-template-by-id (-> context
                           :request
                           :tx-next)
                       (or (:value value) ; for RequestFieldTemplate
                           (:template_id value))))          ;; TODO BUG?
  ([tx tmpl]
   (let [where-clause (sqlp/map->where-clause :procurement_templates tmpl)]
     (-> templates-base-query
         (sql/where where-clause)
         sql-format
         (->> (jdbc/execute! tx))
         ))))

(defn requests-count [{{:keys [tx]} :request} _ {:keys [id]}]
  (-> (sql/select :%count.*)
      (sql/from :procurement_requests)
      (sql/where [:= :template_id [:cast id :uuid]])
      sql-format
      (->> (jdbc/execute! tx))
      :count))
