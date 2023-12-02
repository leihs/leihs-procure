(ns leihs.procurement.resources.request-helpers
  (:require
    ;[clojure.java.jdbc :as jdbc]
    [honey.sql :refer [format] :rename {format sql-format}]

    [honey.sql.helpers :as sql]
    [leihs.core.db :as db]
    [leihs.procurement.utils.sql :as sqlp]
    [next.jdbc :as jdbc]

    ))

(defn join-and-nest-suppliers
  [sqlmap]
  (sqlp/join-and-nest sqlmap
                      :suppliers
                      [:= :suppliers.id :procurement_requests.supplier_id]
                      :supplier))

(defn join-and-nest-categories                              ;OK
  [sqlmap]
  (println ">o>" "join-and-nest-categories")
  (->
    sqlmap
    ;(sql/select
    ;  (:raw
    ;    (str
    ;      "row_to_json(procurement_categories)::jsonb" " || "
    ;      "jsonb_build_object('main_category', row_to_json(procurement_main_categories))"
    ;      " AS category")))

    (sql/select
      [[:raw (str
               "row_to_json(procurement_categories)::jsonb" " || "
               "jsonb_build_object('main_category', row_to_json(procurement_main_categories))"
               " AS category")]])
    (sql/join :procurement_categories
              [:= :procurement_categories.id
               :procurement_requests.category_id])
    (sql/join :procurement_main_categories
              [:= :procurement_main_categories.id
               :procurement_categories.main_category_id])))


(comment
  (let [
        tx (db/get-ds)
        sqlmap (-> (sql/from :procurement_categories :procurement_main_categories))
        query (->
                sqlmap
                (sql/select
                  [[:raw (str
                           "row_to_json(procurement_categories)::jsonb" " || "
                           "jsonb_build_object('main_category', row_to_json(procurement_main_categories))"
                           " AS category")]])

                )

        query (sql-format query)
        p (println "\nquery" query)                         ;clojure-map to json


        ;p (println "\n>o> nested" (pr-str query))
        result (jdbc/execute-one! tx query)

        ;p (println "\nresult" (json/write-str result))      ;clojure-map to json
        p (println "\nresult" result)                       ;clojure-map to json
        ]
    ;working result
    ;result {:category {:id 7d5ba731-edd9-41ba-8773-7337d24c2327, :name Category C1, :cost_center nil, :main_category {:id d2907cbd-98d0-48b7-ab9a-f06803e5cabb, :name Main Category MC1}, :main_category_id d2907cbd-98d0-48b7-ab9a-f06803e5cabb, :procurement_account nil, :general_ledger_account nil}}

    )
  )


(defn join-and-nest-models [sqlmap] (sqlp/select-nest sqlmap :models :model))

(defn join-and-nest-organizations
  [sqlmap]
  (->
    sqlmap
    ;(sql/select
    ;  (:raw
    ;    (str
    ;      "row_to_json(procurement_organizations)::jsonb" " || "
    ;      "jsonb_build_object('department', row_to_json(procurement_departments))"
    ;      " AS organization")))

    (sql/select
      [[:raw
        (str
          "row_to_json(procurement_organizations)::jsonb" " || "
          "jsonb_build_object('department', row_to_json(procurement_departments))"
          " AS organization")]])
    (sql/join :procurement_organizations
              [:= :procurement_organizations.id :procurement_requests.organization_id])
    (sql/join [:procurement_organizations :procurement_departments]
              [:= :procurement_departments.id :procurement_organizations.parent_id])))




(comment
  (let [
        tx (db/get-ds)

        ;sqlmap (-> (sql/from :procurement_organizations))
        ;query (->
        ;        sqlmap
        ;        (sql/select
        ;          [[:raw
        ;            (str
        ;              "row_to_json(procurement_organizations)::jsonb"
        ;              " AS organization")]])
        ;        )

        ;========================

        ;; FYI: had to create :procurement_departments manually baseed on :procurement_organizations
        sqlmap (-> (sql/from :procurement_organizations :procurement_departments))
        query (->
                sqlmap
                (sql/select
                  [[:raw
                    (str
                      "row_to_json(procurement_organizations)::jsonb" " || "
                      "jsonb_build_object('department', row_to_json(procurement_departments))"
                      " AS organization")]])
                )

        query (sql-format query)
        p (println "\nquery" query)                         ;clojure-map to json


        ;p (println "\n>o> nested" (pr-str query))
        result (jdbc/execute-one! tx query)

        ;p (println "\nresult" (json/write-str result))      ;clojure-map to json
        p (println "\nresult" result)                       ;clojure-map to json
        ]
    ;working result
    ;query [SELECT row_to_json(procurement_organizations)::jsonb || jsonb_build_object('department', row_to_json(procurement_departments)) AS organization FROM procurement_organizations, procurement_departments]
    )

  )







(defn join-and-nest-budget-periods
  [sqlmap]
  (println ">o>" "join-and-nest-budget-periods")
  (-> sqlmap
      (sqlp/select-nest :procurement_budget_periods_2 :budget_period)
      (sql/join
        ;[(-> (sql/select :* [(:> :current_date :end_date) :is_past]) ;; FIXME
        [(-> (sql/select :* [[:> :current_date :end_date] :is_past]) ;; FIXME
             (sql/from :procurement_budget_periods))

         :procurement_budget_periods_2]
        [:= :procurement_budget_periods_2.id :procurement_requests.budget_period_id]))

  )





(comment
  (let [
        tx (db/get-ds)

        ;; FYI: had to create :procurement_departments manually baseed on :procurement_organizations
        ;sqlmap (-> (sql/from :procurement_budget_periods :procurement_requests))
        sqlmap (-> (sql/from :procurement_requests))
        query   (-> sqlmap
                    (sqlp/select-nest :procurement_budget_periods :budget_period)
                    (sql/join
                      ;[(-> (sql/select :* [(:> :current_date :end_date) :is_past]) ;; FIXME

                      ;[(-> (sql/select :* [[:> :current_date :end_date] :is_past]) ;; FIXME
                      [(-> (sql/select :*) ;; FIXME
                           (sql/from :procurement_budget_periods)
                           (sql/where [[:> :current_date :end_date] :is_past])
                           )

                       :procurement_budget_periods]
                      [:= :procurement_budget_periods.id :procurement_requests.budget_period_id]))


        query (sql-format query)
        p (println "\nquery" query)                         ;clojure-map to json


        ;p (println "\n>o> nested" (pr-str query))
        result (jdbc/execute-one! tx query)

        ;p (println "\nresult" (json/write-str result))      ;clojure-map to json
        p (println "\nresult" result)                       ;clojure-map to json
        ]
    ;working result
    ;query [SELECT row_to_json(procurement_organizations)::jsonb || jsonb_build_object('department', row_to_json(procurement_departments)) AS organization FROM procurement_organizations, procurement_departments]
    )

  )






(defn join-and-nest-templates
  [sqlmap]
  (println ">o>" "join-and-nest-templates")
  (sqlp/join-and-nest sqlmap
                      :procurement_templates
                      [:= :procurement_templates.id
                       :procurement_requests.template_id]
                      :template))

(defn join-and-nest-rooms
  [sqlmap]
  (-> sqlmap
      ;(sql/select
      ;  (:raw (str "row_to_json(rooms)::jsonb" " || "
      ;             "jsonb_build_object('building', row_to_json(buildings))"
      ;             " AS room")))

      (sql/select
        [[:raw (str "row_to_json(rooms)::jsonb" " || "
                    "jsonb_build_object('building', row_to_json(buildings))"
                    " AS room")]])
      (sql/join :rooms [:= :rooms.id :procurement_requests.room_id])
      (sql/join :buildings [:= :buildings.id :rooms.building_id])))




(comment
  (let [
        tx (db/get-ds)

        ;sqlmap (-> (sql/from :procurement_organizations))
        ;query (->
        ;        sqlmap
        ;        (sql/select
        ;          [[:raw
        ;            (str
        ;              "row_to_json(procurement_organizations)::jsonb"
        ;              " AS organization")]])
        ;        )

        ;========================

        ;; FYI: had to create :procurement_departments manually baseed on :procurement_organizations
        sqlmap (-> (sql/from :rooms :buildings))
        query (->
                sqlmap
                (sql/select
                  [[:raw (str "row_to_json(rooms)::jsonb" " || "
                              "jsonb_build_object('building', row_to_json(buildings))"
                              " AS room")]])

                ;(sql/select
                ;  (:raw (str "row_to_json(rooms)::jsonb" " || "
                ;             "jsonb_build_object('building', row_to_json(buildings))"
                ;             " AS room")))
                )

        query (sql-format query)
        p (println "\nquery" query)                         ;clojure-map to json


        ;p (println "\n>o> nested" (pr-str query))
        result (jdbc/execute-one! tx query)

        ;p (println "\nresult" (json/write-str result))      ;clojure-map to json
        p (println "\nresult" result)                       ;clojure-map to json
        ]
    ;working result
    ;{:room {:id b8927fd2-014c-48c2-b095-fa1d5620debe, :name general room, :general true, :building {:id abae04c5-d767-425e-acc2-7ce04df645d1, :code nil, :name general building}, :building_id abae04c5-d767-425e-acc2-7ce04df645d1, :description nil}}

    )

  )

















(defn join-and-nest-users
  [sqlmap]
  (println ">o>" "join-and-nest-users")
  (sqlp/join-and-nest sqlmap
                      :users
                      [:= :users.id :procurement_requests.user_id]
                      :user))

(defn join-and-nest-associated-resources
  [sqlmap]
  (-> sqlmap
      join-and-nest-budget-periods

      ;; TODO: fix me _> NULL values
      join-and-nest-categories
      join-and-nest-models
      join-and-nest-organizations
      join-and-nest-templates
      join-and-nest-rooms
      join-and-nest-suppliers
      join-and-nest-users))
