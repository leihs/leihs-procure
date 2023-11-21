(ns leihs.procurement.resources.template
  (:require
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [leihs.procurement.utils.sql :as sqlp]
    [next.jdbc :as jdbc]
    [taoensso.timbre :refer [debug error info spy warn]]
    [taoensso.timbre :refer [debug error info warn]]))

(def ALLOWED-KEYS-FOR-USED-TEMPLATE #{:is_archived})

(def templates-base-query
  (-> (sql/select :procurement_templates.*)
      (sql/from :procurement_templates)))

(defn get-template-by-id
  [tx id]
  (-> templates-base-query
      (sql/where [:= :procurement_templates.id [:cast id :uuid]])
      sql-format
      (->> (jdbc/execute-one! tx))))

(defn insert-template!
  [tx tmpl]
  (let [result (-> (jdbc/execute! tx (-> (sql/insert-into :procurement_templates)
                                         (sql/values [tmpl])
                                         sql-format)))
        count (:update-count result)]
    (:update-count count)))

(defn validate-update-attributes [tx tmpl]
  (let [result (-> (sql/select :%count.*)
                       (sql/from :procurement_requests)
                       (sql/where [:= :template_id (:id tmpl)])
                       sql-format
                       (->> (jdbc/execute-one! tx)))
        req-exist? (-> result
                       :count
                       (> 0))]
    (if req-exist?
      (select-keys tmpl ALLOWED-KEYS-FOR-USED-TEMPLATE))
    tmpl))

(defn update-template!
  [tx tmpl]
  (let [casted-tmpl (validate-update-attributes tx tmpl)]
    (jdbc/execute-one! tx
                 (-> (sql/update :procurement_templates)
                     (sql/set casted-tmpl)
                     (sql/where [:= :procurement_templates.id (:id tmpl)])
                     sql-format))))

(defn delete-template!
  [tx id]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_templates)
                     (sql/where [:= :procurement_templates.id id])
                     sql-format)))

(defn get-template
  ([context _ value]
   (get-template-by-id (-> context
                           :request
                           :tx-next)
                       (or (:value value) ; for RequestFieldTemplate
                           (:template_id value))))
  ([tx tmpl]
   (let [where-clause (sqlp/map->where-clause :procurement_templates tmpl)]
     (-> templates-base-query
         (sql/where where-clause)
         sql-format
         (->> (jdbc/execute! tx))))))

(defn requests-count [{{:keys [tx]} :request} _ {:keys [id]}]
  (-> (sql/select :%count.*)
      (sql/from :procurement_requests)
      (sql/where [:= :template_id id])
      sql-format
      (->> (jdbc/execute-one! tx))
      :count))
