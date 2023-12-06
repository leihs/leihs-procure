(ns leihs.procurement.resources.templates
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.authorization :as authorization]
            [leihs.procurement.permissions.user :as user-perms]
            [leihs.procurement.resources [categories :as categories]
             [template :as template]]
            [taoensso.timbre :refer [debug info warn error spy]]
            [leihs.core.db :as db]

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

  ;./spec/features/templates/add_template_spec.rb:18
  ;>oo> templates::get-templates1a ?broken-base-query? {:select 2023-12-06T15:38:24.741Z NX-41294 DEBUG [leihs.procurement.permissions.user:52]
  ; - (-> (sql/select [(sql/call :exists (cond-> (-> (sql/select true) (sql/from :procurement_category_viewers)
  ; (sql/merge-where [:= :procurement_category_viewers.user_id (:user_id auth-entity)])) c-id (sql/merge-where
  ; [:= :procurement_category_viewers.category_id c-id]))) :result]) sql/format) =>
  ;
  ; ["SELECT exists((SELECT TRUE FROM procurement_category_viewers WHERE procurement_category_viewers.user_id = ?))
  ; AS result " #uuid "16a60ffd-f39b-40bf-aa7e-2c67bdb9943c"]

  (println ">oo> templates::get-templates2 _> HERE value contains :id??)" (:id value) value)
  (let [query (cond-> templates-base-query
                      value (sql/merge-where [:= :procurement_templates.category_id (:id value)]))
        p (println ">oo> templates::get-templates1a ?broken-base-query?" templates-base-query)
        p (println ">oo> templates::get-templates1b ?broken-base-query?" (-> templates-base-query sql/format))
        p (println ">oo> templates::get-templates1c" (sql/format query))
        ]
    (->> (spy query)
         sql/format
         (jdbc/query (spy (-> context
                              :request
                              :tx))))))


(comment

  ;>o> result [SELECT procurement_templates.* FROM procurement_templates  ORDER BY concat((lower(coalesce(procurement_templates.article_name, ?)),
  ;                                                                                             lower(coalesce(procurement_templates.supplier_name, ?))))  ]


  (let [

        tx (db/get-ds)

        result (-> (sql/select :procurement_templates.*)
                   (sql/from :procurement_templates)

                   (sql/order-by (->> [:procurement_templates.article_name :procurement_templates.supplier_name]
                                      (map #(->> (sql/call :coalesce % "")
                                                 (sql/call :lower)))
                                      (sql/call :concat)))
                   sql/format
                   )

        p (println ">o> result" result)
        p (println "\n>o> result" (jdbc/query tx result))

        ]
    )

  )

(comment

  (let [
        tx (db/get-ds)
        request {:route-params {:user-id #uuid "c0777d74-668b-5e01-abb5-f8277baa0ea8"}
                 :tx tx}

        data {:id #uuid "80226a51-c17a-5fd8-93db-40aef5b03491"}
        result (get-templates {:request request} nil data)

        p (println "\nresult" result)
        ]
    result
    )
  )

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
              #(or (user-perms/admin? tx auth-entity)
                   (user-perms/inspector? tx auth-entity (:category_id tmpl))))
            (->> tmpl
                 (get-template-id tx)
                 (conj tmpl-ids)
                 (recur rest-tmpls)))
        (categories/get-categories-for-ids tx cat-ids)))))
