(ns leihs.procurement.resources.templates
  (:require
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [leihs.core.db :as db]
    [leihs.procurement.authorization :as authorization]
    [leihs.procurement.permissions.user :as user-perms]
    (leihs.procurement.resources [categories :as categories]
                                 [template :as template])

    [leihs.procurement.utils.helpers :refer [add-comment-to-sql-format cast-uuids]]
    [next.jdbc :as jdbc]
    [taoensso.timbre :refer [debug error info spy warn]]
    ))

(do
  (def templates-base-query
    (-> (sql/select :procurement_templates.*)
        (sql/from :procurement_templates)
        (sql/left-join :models [:= :models.id :procurement_templates.model_id])
        (sql/order-by [[:concat (->> [:procurement_templates.article_name :models.product :models.version]
                                     (map #(->> [:lower [:coalesce % ""]])))
                        ]])
        ))
  )

(defn get-templates
  [context _ value]
  (let [query (cond-> templates-base-query
                      value (sql/where [:= :procurement_templates.category_id [:cast (:id value) :uuid]]))]
    (->> query
         sql-format
         (jdbc/execute! (-> context
                            :request
                            :tx-next))))

  )


(comment

  ;>o> result [SELECT procurement_templates.* FROM procurement_templates  ORDER BY concat((lower(coalesce(procurement_templates.article_name, ?)),
  ;                                                                                             lower(coalesce(procurement_templates.supplier_name, ?))))  ]

  (let [

        tx (db/get-ds-next)

        result (-> (sql/select :procurement_templates.*)
                   (sql/from :procurement_templates)

                   ;; works
                   (sql/order-by [[:concat (->> [:procurement_templates.article_name :procurement_templates.supplier_name]
                                                (map #(->> [:lower [:coalesce % ""]])))
                                   ]])

                   ;; works
                   ;(sql/order-by [[:concat
                   ;                [:lower [:coalesce :procurement_templates.article_name ""]]
                   ;                [:lower [:coalesce :procurement_templates.supplier_name ""]]
                   ;                ]]
                   ;              )

                   sql-format)

        p (println ">oo> result" result)
        p (println "\n>oo> result" (jdbc/execute! tx result))

        ]
    )
  )


(defn get-templates-for-ids
  [tx ids]
  (jdbc/execute! tx (add-comment-to-sql-format (-> categories/categories-base-query
                                                   (sql/where [:in :procurement_categories.id (cast-uuids ids)])
                                                   sql-format)) "templates/get-templates-for-ids"))



(defn delete-templates-not-in-ids!
  [tx ids]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_templates)
                     (sql/where [:not-in :procurement_templates.id (cast-uuids ids)])
                     sql-format)))

(defn get-template-id
  [tx tmpl]
  (or (:id tmpl)
      (as-> tmpl <> (dissoc <> :id) (template/get-template tx <>) (:id <>))))


(defn update-templates!
  [context args _]
  (let [rrequest (:request context)
        tx (:tx-next rrequest)
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

                 (template/insert-template! tx (dissoc tmpl :id))
                 )
              :if-only
              #(or (user-perms/admin? tx auth-entity)
                   (user-perms/inspector? tx auth-entity (:category_id tmpl))))
            (->> tmpl
                 (get-template-id tx)
                 (conj tmpl-ids)
                 (recur rest-tmpls)))
        (categories/get-categories-for-ids tx cat-ids)))))
