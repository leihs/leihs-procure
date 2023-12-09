(ns leihs.procurement.resources.templates
  (:require

    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]


        [taoensso.timbre :refer [debug info warn error spy]]


    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]


    [leihs.procurement.authorization :as authorization]
    [leihs.procurement.permissions.user :as user-perms]
    [leihs.procurement.resources [categories :as categories]
     [template :as template]]
    ))

(do
  (println ">o> templates::templates-base-query ERROR?")

  (def templates-base-query
    (spy (-> (sql/select :procurement_templates.*)
        (sql/from :procurement_templates)
        (sql/left-join :models [:= :models.id :procurement_templates.model_id])
        (sql/order-by [[:concat (->> [:procurement_templates.article_name :models.product :models.version]
                                     (map #(->> [:lower [:coalesce % ""]])))
                        ]])
        )))
  )

(defn get-templates
  [context _ value]

  ;./spec/features/templates/add_template_spec.rb:18
  ;>oo> templates::get-templates1a ?broken-base-query? {:select [:procurement_templates.*], :from [:procurement_templates], :left-join [:models [:= :models.id :procurement_templates.model_id]], :order-by [nil]}

  (println ">oo> templates::get-templates2 _> HERE value contains :id??)" (:id value) value)
  (let [query (cond-> templates-base-query
                      value (sql/where [:= :procurement_templates.category_id [:cast (:id value) :uuid]]))
        p (println ">oo> templates::get-templates1a ?broken-base-query?" templates-base-query)
        p (println ">oo> templates::get-templates1b ?broken-base-query?" (-> templates-base-query sql-format))
        p (println ">oo> templates::get-templates1c" (sql-format query))
        ]
    (->> query
         sql-format
         spy
         (jdbc/execute! (-> context
                            :request
                            :tx-next)))))


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

        p (println ">o> result" result)
        p (println "\n>o> result" (jdbc/execute! tx result))

        ]
    )
  )

(defn get-templates-for-ids
  [tx ids]
  (-> categories/categories-base-query
      (sql/where [:in :procurement_categories.id ids])      ;; TODO PRIO !!!
      sql-format
      (->> (jdbc/execute! tx))))

(defn delete-templates-not-in-ids!
  [tx ids]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_templates)
                     (sql/where [:not-in :procurement_templates.id ids]) ;; TODO PRIO !!!
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
    (loop [[tmpl & rest-tmpls] (spy input-data)
           tmpl-ids []]
      ;(println ">o> templates " tmpl)
      (if (spy tmpl)
        (do (authorization/authorize-and-apply
              #(if-let [id (:id tmpl)]
                 (if (spy (:to_delete tmpl))
                   (spy (template/delete-template! tx id))
                   (spy (template/update-template! tx tmpl)))

                 (spy (template/insert-template! tx (dissoc tmpl :id)))
                 )
              :if-only
              #(or (user-perms/admin? tx auth-entity)
                   (user-perms/inspector? tx auth-entity (:category_id tmpl))))
            (->> tmpl
                 (get-template-id tx)
                 (conj tmpl-ids)
                 (recur rest-tmpls)))
        (categories/get-categories-for-ids tx cat-ids)))))
