(ns leihs.procurement.resources.request-helpers
  (:require
    [honey.sql.helpers :as sql]
    [leihs.procurement.utils.sql :as sqlp]))

(defn join-and-nest-suppliers
  [sqlmap]
  (sqlp/join-and-nest sqlmap
                      :suppliers
                      [:= :suppliers.id :procurement_requests.supplier_id] :supplier))

(defn join-and-nest-categories
  [sqlmap]
  (->
    sqlmap
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

(defn join-and-nest-models [sqlmap] (sqlp/select-nest sqlmap :models :model))

(defn join-and-nest-organizations
  [sqlmap]
  (->
    sqlmap
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

(defn join-and-nest-budget-periods
  [sqlmap]
  (-> sqlmap
      (sqlp/select-nest :procurement_budget_periods_2 :budget_period)
      (sql/join
        [(-> (sql/select :* [[:> :current_date :end_date] :is_past])
             (sql/from :procurement_budget_periods))
         :procurement_budget_periods_2]
        [:= :procurement_budget_periods_2.id
         :procurement_requests.budget_period_id])))

(defn join-and-nest-templates
  [sqlmap]
  (sqlp/join-and-nest sqlmap
                      :procurement_templates
                      [:= :procurement_templates.id
                       :procurement_requests.template_id]
                      :template))

(defn join-and-nest-rooms
  [sqlmap]
  (-> sqlmap
      (sql/select [[:raw (str "row_to_json(rooms)::jsonb" " || "
                              "jsonb_build_object('building', row_to_json(buildings))"
                              " AS room")]])
      (sql/join :rooms [:= :rooms.id :procurement_requests.room_id])
      (sql/join :buildings [:= :buildings.id :rooms.building_id])))

(defn join-and-nest-users
  [sqlmap]
  (sqlp/join-and-nest sqlmap
                      :users
                      [:= :users.id :procurement_requests.user_id] :user))

(defn join-and-nest-associated-resources
  [sqlmap]
  (-> sqlmap
      join-and-nest-budget-periods
      join-and-nest-categories
      join-and-nest-models
      join-and-nest-organizations
      join-and-nest-templates
      join-and-nest-rooms
      join-and-nest-suppliers
      join-and-nest-users))
