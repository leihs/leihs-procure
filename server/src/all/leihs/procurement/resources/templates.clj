(ns leihs.procurement.resources.templates
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.authorization :as authorization]
            [leihs.procurement.permissions.user :as user-perms]
            [leihs.procurement.resources [categories :as categories]
             [template :as template]]
            [leihs.procurement.utils.sql :as sql]))

(def templates-base-query
  (-> (sql/select :procurement_templates.*)
      (sql/from :procurement_templates)
      (sql/merge-left-join :models
                           [:= :models.id :procurement_templates.model_id])
      (sql/order-by (->> [:procurement_templates.article_name :models.product
                          :models.version]
                         (map #(->> (sql/call :coalesce % "")
                                    (sql/call :lower)))
                         (sql/call :concat)))))

(defn get-templates
  [context _ value]
  (let [query (cond-> templates-base-query
                value (sql/merge-where [:= :procurement_templates.category_id
                                        (:id value)]))]
    (->> query
         sql/format
         (jdbc/query (-> context
                         :request
                         :tx)))))

(defn get-templates-for-ids
  [tx ids]
  (-> categories/categories-base-query
      (sql/merge-where [:in :procurement_categories.id ids])
      sql/format
      (->> (jdbc/query tx))))

(defn delete-templates-not-in-ids!
  [tx ids]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_templates)
                     (sql/where [:not-in :procurement_templates.id ids])
                     sql/format)))

(defn get-template-id
  [tx tmpl]
  (or (:id tmpl)
      (as-> tmpl <> (dissoc <> :id) (template/get-template tx <>) (:id <>))))

(defn update-templates!
  [context args _]
  (let [rrequest (:request context)
        tx (:tx rrequest)
        auth-entity (:authenticated-entity rrequest)
        input-data (:input_data args)
        cat-ids (map :category_id input-data)]
    (loop [[tmpl & rest-tmpls] input-data
           tmpl-ids []]
      (if tmpl
        (do (authorization/authorize-and-apply
              #(if-let [id (:id tmpl)]
                (if (:to_delete tmpl)
                  (template/delete-template! tx id)
                  (template/update-template! tx tmpl))
                (template/insert-template! tx (dissoc tmpl :id)))
              :if-only
              #(user-perms/inspector? tx auth-entity (:category_id tmpl)))
            (->> tmpl
                 (get-template-id tx)
                 (conj tmpl-ids)
                 (recur rest-tmpls)))
        (categories/get-categories-for-ids tx cat-ids)))))
