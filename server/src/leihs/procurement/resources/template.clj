(ns leihs.procurement.resources.template
  (:require

    ;[clojure.java.jdbc :as jdbc]
    [leihs.procurement.utils.sql :as sqlp]

    [leihs.core.utils :refer [my-cast]]

    [taoensso.timbre :refer [debug info warn error spy]]


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
  (spy (-> templates-base-query
           (sql/where [:= :procurement_templates.id [:cast id :uuid]])
           sql-format
           (->> (jdbc/execute-one! tx)))))


(defn insert-template!
  [tx tmpl]
  (let [tmpl (my-cast tmpl)
        result (spy (-> (jdbc/execute! tx (-> (sql/insert-into :procurement_templates)
                                              (sql/values [tmpl])
                                              sql-format))))
        result (:update-count result)]
    (:update-count result)))

(defn validate-update-attributes [tx tmpl]
  (let [
        result (-> (sql/select :%count.*)
                   (sql/from :procurement_requests)
                   ;(sql/where [:= :template_id [:cast (:id tmpl) :uuid]])
                   (sql/where [:= :template_id (:id tmpl)])
                   sql-format
                   (->> (jdbc/execute-one! tx))
                   )


        req-exist? (-> (spy result)
                       :count
                       (> 0)
                       )
        ]
    (if req-exist?
      (select-keys tmpl ALLOWED-KEYS-FOR-USED-TEMPLATE))

    tmpl))





(defn update-template!
  [tx tmpl]
  (let [casted-tmpl (my-cast (spy tmpl))
        casted-tmpl (validate-update-attributes tx casted-tmpl)]
    (-> (jdbc/execute-one! tx (-> (sql/update :procurement_templates)
                                  (sql/set (spy casted-tmpl))
                                  (sql/where [:= :procurement_templates.id (:id casted-tmpl)])
                                  sql-format))
        :next.jdbc/update-count
        list
        )))

(defn delete-template!
  [tx id]
  (jdbc/execute! tx (-> (sql/delete-from :procurement_templates)
                        (sql/where [:= :procurement_templates.id [:cast id :uuid]])
                        sql-format)))

(defn get-template
  ([context _ value]
   (get-template-by-id (-> context
                           :request
                           :tx-next)
                       (or (:value value)                   ; for RequestFieldTemplate
                           (:template_id value))))

  ([tx tmpl]
   (let [
         tmpl (my-cast tmpl)
         where-clause (sqlp/map->where-clause :procurement_templates (spy tmpl))]
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
      (->> (jdbc/execute-one! tx))
      :count))
