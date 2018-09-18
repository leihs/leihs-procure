(ns leihs.procurement.resources.request-helpers
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.utils.sql :as sql]))

(defn join-and-nest-suppliers
  [sqlmap]
  (sql/join-and-nest sqlmap
                     :suppliers
                     [:= :suppliers.id :procurement_requests.supplier_id]
                     :supplier))

(defn join-and-nest-categories
  [sqlmap]
  (->
    sqlmap
    (sql/merge-select
      (sql/raw
        (str
          "row_to_json(procurement_categories)::jsonb" " || "
          "jsonb_build_object('main_category', row_to_json(procurement_main_categories))"
            " AS category")))
    (sql/merge-join :procurement_categories
                    [:= :procurement_categories.id
                     :procurement_requests.category_id])
    (sql/merge-join :procurement_main_categories
                    [:= :procurement_main_categories.id
                     :procurement_categories.main_category_id])))

(defn join-and-nest-models [sqlmap] (sql/select-nest sqlmap :models :model))

(defn join-and-nest-organizations
  [sqlmap]
  (->
    sqlmap
    (sql/merge-select
      (sql/raw
        (str
          "row_to_json(procurement_organizations)::jsonb" " || "
          "jsonb_build_object('department', row_to_json(procurement_departments))"
            " AS organization")))
    (sql/merge-join :procurement_organizations
                    [:= :procurement_organizations.id
                     :procurement_requests.organization_id])
    (sql/merge-join [:procurement_organizations :procurement_departments]
                    [:= :procurement_departments.id
                     :procurement_organizations.parent_id])))

(defn join-and-nest-budget-periods
  [sqlmap]
  (sql/select-nest sqlmap :procurement_budget_periods :budget_period))

(defn join-and-nest-templates
  [sqlmap]
  (sql/join-and-nest sqlmap
                     :procurement_templates
                     [:= :procurement_templates.id
                      :procurement_requests.template_id]
                     :template))

(defn join-and-nest-rooms
  [sqlmap]
  (-> sqlmap
      (sql/merge-select
        (sql/raw (str "row_to_json(rooms)::jsonb" " || "
                      "jsonb_build_object('building', row_to_json(buildings))"
                        " AS room")))
      (sql/merge-join :rooms [:= :rooms.id :procurement_requests.room_id])
      (sql/merge-join :buildings [:= :buildings.id :rooms.building_id])))


(defn join-and-nest-users
  [sqlmap]
  (sql/join-and-nest sqlmap
                     :users
                     [:= :users.id :procurement_requests.user_id]
                     :user))

(defn join-and-nest-associated-resources
  [sqlmap]
  (-> sqlmap
      join-and-nest-suppliers
      join-and-nest-categories
      join-and-nest-models
      join-and-nest-organizations
      join-and-nest-budget-periods
      ; NOTE: join-and-nest-templates -> no need at the moment
      join-and-nest-rooms
      join-and-nest-users))
