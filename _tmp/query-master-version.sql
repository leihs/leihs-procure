SELECT DISTINCT ON (
    procurement_requests.id,
    concat(
            lower(coalesce(procurement_requests.article_name, '')),
            lower(coalesce(models.product, '')),
            lower(coalesce(models.version, ''))
        )
    ) procurement_requests.*,
      CASE
          WHEN procurement_requests.approved_quantity IS NULL THEN 'NEW'
          WHEN procurement_requests.approved_quantity >= procurement_requests.requested_quantity THEN 'APPROVED'
          WHEN procurement_requests.approved_quantity < procurement_requests.requested_quantity AND
               procurement_requests.approved_quantity > 0 THEN 'PARTIALLY_APPROVED'
          WHEN procurement_requests.approved_quantity = 0 THEN 'DENIED'
          END                                                                             AS state,

      row_to_json(procurement_budget_periods_2)                                           AS budget_period,

      row_to_json(procurement_categories)::jsonb ||
      jsonb_build_object('main_category', row_to_json(procurement_main_categories))       AS category,

      row_to_json(models)                                                                 AS model,
      row_to_json(procurement_organizations)::jsonb ||
      jsonb_build_object('department', row_to_json(procurement_departments))              AS organization,
      row_to_json(procurement_templates)                                                  AS template,
      row_to_json(rooms)::jsonb || jsonb_build_object('building', row_to_json(buildings)) AS room,
      row_to_json(suppliers)                                                              AS supplier,
      row_to_json(users)                                                                  AS user

FROM procurement_requests
         INNER JOIN procurement_budget_periods ON procurement_budget_periods.id = procurement_requests.budget_period_id

         INNER JOIN (SELECT *, current_date > end_date AS is_past
                     FROM procurement_budget_periods) procurement_budget_periods_2
                    ON procurement_budget_periods_2.id = procurement_requests.budget_period_id

         INNER JOIN procurement_categories ON procurement_categories.id = procurement_requests.category_id
         INNER JOIN procurement_main_categories
                    ON procurement_main_categories.id = procurement_categories.main_category_id
         INNER JOIN procurement_organizations ON procurement_organizations.id = procurement_requests.organization_id

         INNER JOIN procurement_organizations procurement_departments
                    ON procurement_departments.id = procurement_organizations.parent_id

         INNER JOIN rooms ON rooms.id = procurement_requests.room_id
         INNER JOIN buildings ON buildings.id = rooms.building_id
         LEFT JOIN models ON models.id = procurement_requests.model_id
         LEFT JOIN procurement_templates ON procurement_templates.id = procurement_requests.template_id
         LEFT JOIN suppliers ON suppliers.id = procurement_requests.supplier_id
         LEFT JOIN users ON users.id = procurement_requests.user_id

WHERE (procurement_requests.category_id in ('7d5ba731-edd9-41ba-8773-7337d24c2327')
    AND procurement_requests.budget_period_id in ('8b8fe440-cae5-4bf9-8048-d0ec2399faa1')
    AND procurement_requests.organization_id in ('fb664326-a8ef-4556-af02-07d3127cd9ec')
    AND procurement_requests.priority in ('normal')
    AND procurement_requests.inspector_priority in ('high')
    AND (
               procurement_requests.approved_quantity IS NULL
               OR procurement_requests.approved_quantity >= procurement_requests.requested_quantity
               OR (procurement_requests.approved_quantity < procurement_requests.requested_quantity
               AND procurement_requests.approved_quantity > 0)
               OR procurement_requests.approved_quantity = 0)

    AND procurement_requests.order_status in (
                                              CAST('not_processed' AS order_status_enum),
                                              CAST('in_progress' AS order_status_enum),
                                              CAST('procured' AS order_status_enum),
                                              CAST('alternative_procured' AS order_status_enum),
                                              CAST('not_procured' AS order_status_enum)))

ORDER BY concat(
                 lower(coalesce(procurement_requests.article_name, '')),
                 lower(coalesce(models.product, '')),
                 lower(coalesce(models.version, ''))
             )