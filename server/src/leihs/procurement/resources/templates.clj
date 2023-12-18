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
    (spy (->> (spy query)
         sql/format
         spy
         (jdbc/query (-> context
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
  (spy (-> categories/categories-base-query
      (sql/merge-where [:in :procurement_categories.id ids])
      sql/format
      (->> (jdbc/query tx)))))

(defn delete-templates-not-in-ids!
  [tx ids]
  (spy (jdbc/execute! tx (-> (sql/delete-from :procurement_templates)
                             (sql/where [:not-in :procurement_templates.id ids])
                             sql/format))))

(defn get-template-id
  [tx tmpl]
  (spy (or (spy (:id tmpl))
      (spy (as-> tmpl <> (dissoc <> :id) (template/get-template tx <>) (:id <>))))))

(defn update-templates!
  [context args _]
  (spy (let [rrequest (:request context)
        tx (:tx rrequest)
        auth-entity (:authenticated-entity rrequest)
        input-data (:input_data args)
        cat-ids (map :category_id input-data)]
    (loop [[tmpl & rest-tmpls] (spy input-data)
           tmpl-ids []]
      (println ">o> templates " tmpl)
      (if (spy tmpl)
        (do (authorization/authorize-and-apply
              (spy #(if-let [id (:id tmpl)]
                 (if (spy (:to_delete tmpl))
                   (spy (template/delete-template! tx id))
                   (spy (template/update-template! tx tmpl)) ;;here
                   )

                 (spy (template/insert-template! tx (dissoc tmpl :id)))
                 ))
              :if-only
              #(or (spy (user-perms/admin? tx auth-entity))
                   (spy (user-perms/inspector? tx auth-entity (:category_id tmpl)))))
            (->> tmpl
                 (get-template-id tx)
                 (conj tmpl-ids)
                 spy
                 (recur rest-tmpls)))
        (spy (categories/get-categories-for-ids tx cat-ids))

        )))))


;templates
;mutation
;>> result: {"data"=>{"update_templates"=>nil}, "errors"=>[{"message"=>"UnauthorizedException - Not authorized for this query path and arguments.", "locations"=>[{"line"=>2, "column"=>11}], "path"=>["update_templates"], "extensions"=>{"exception"=>"ExceptionInfo", "arguments"=>{"input_data"=>[{"id"=>"f864bfae-fdf7-4e00-a32b-029bb4a40e9f", "article_name"=>"test", "category_id"=>"7881e996-71b8-4061-bb63-bba6fc6aa443", "price_cents"=>100}, {"id"=>"4f43e88a-5b68-46ec-abfa-a19f609cab87", "article_name"=>"test", "category_id"=>"e8a70264-5d35-4f2e-a71b-71e31fbfe421", "price_cents"=>100}]}}}]}
;>>> data={:article_name=>"tmpl for category A", :category_id=>"7881e996-71b8-4061-bb63-bba6fc6aa443"}
;>>> Template.find(data)=#<Template:0x0000000114884428>
;>>> data={:article_name=>"tmpl for category B", :category_id=>"e8a70264-5d35-4f2e-a71b-71e31fbfe421"}
;>>> Template.find(data)=#<Template:0x000000011487ec08>
;throws if not inspector of some category


;>o> templates  {:id f864bfae-fdf7-4e00-a32b-029bb4a40e9f, :article_name test, :category_id 7881e996-71b8-4061-bb63-bba6fc6aa443, :price_cents 100}
;2023-12-16T18:09:22.538Z NX-41294 DEBUG [leihs.procurement.resources.templates:122] - tmpl => {:id "f864bfae-fdf7-4e00-a32b-029bb4a40e9f", :article_name "test", :category_id "7881e996-71b8-4061-bb63-bba6fc6aa443", :price_cents 100}
;2023-12-16T18:09:22.540Z NX-41294 DEBUG [leihs.procurement.resources.templates:125] - (:to_delete tmpl) => nil
;2023-12-16T18:09:22.541Z NX-41294 DEBUG [leihs.procurement.resources.template:37] - tmpl => {:id "f864bfae-fdf7-4e00-a32b-029bb4a40e9f", :article_name "test", :category_id "7881e996-71b8-4061-bb63-bba6fc6aa443", :price_cents 100}
;2023-12-16T18:09:22.544Z NX-41294 DEBUG [leihs.procurement.resources.template:35] - (-> (sql/select :%count.*) (sql/from :procurement_requests) (sql/where [:= :template_id (:id (spy tmpl))]) sql/format (->> (jdbc/query tx)) first :count (> 0)) => false
;2023-12-16T18:09:22.544Z NX-41294 DEBUG [leihs.procurement.resources.template:41] - req-exist? => false
;2023-12-16T18:09:22.562Z NX-41294 DEBUG [leihs.procurement.resources.template:47] - (jdbc/execute! tx (-> (sql/update :procurement_templates) (sql/sset (validate-update-attributes tx tmpl)) (sql/where [:= :procurement_templates.id (:id tmpl)]) sql/format)) => (1)
;2023-12-16T18:09:22.563Z NX-41294 DEBUG [leihs.procurement.resources.templates:127] - (template/update-template! tx tmpl) => (1)
;2023-12-16T18:09:22.564Z NX-41294 DEBUG [leihs.procurement.resources.templates:109] - (:id tmpl) => f864bfae-fdf7-4e00-a32b-029bb4a40e9f
;2023-12-16T18:09:22.564Z NX-41294 DEBUG [leihs.procurement.resources.templates:?] - (conj tmpl-ids (get-template-id tx tmpl)) => ["f864bfae-fdf7-4e00-a32b-029bb4a40e9f"]