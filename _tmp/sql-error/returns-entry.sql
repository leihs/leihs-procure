SELECT DISTINCT ON (
    procurement_requests.id,
    concat(
            lower(coalesce(procurement_requests.article_name, '')),
            lower(coalesce(models.product, '')),
            lower(coalesce(models.version, ''))
        )
    )
    procurement_requests.*,
    (
        CASE
            WHEN procurement_requests.approved_quantity IS NULL THEN 'NEW'
            WHEN procurement_requests.approved_quantity >= procurement_requests.requested_quantity THEN 'APPROVED'
            WHEN (procurement_requests.approved_quantity < procurement_requests.requested_quantity) AND (procurement_requests.approved_quantity > 0) THEN 'PARTIALLY_APPROVED'
            WHEN procurement_requests.approved_quantity = 0 THEN 'DENIED'
            END
        ) AS state,
    (ROW_TO_JSON(procurement_budget_periods_2)) AS budget_period,
    row_to_json(procurement_categories)::jsonb || jsonb_build_object('main_category', row_to_json(procurement_main_categories)) AS category,
    (ROW_TO_JSON(models)) AS model,
    row_to_json(procurement_organizations)::jsonb || jsonb_build_object('department', row_to_json(procurement_departments)) AS organization,
    (ROW_TO_JSON(procurement_templates)) AS template,
    row_to_json(rooms)::jsonb || jsonb_build_object('building', row_to_json(buildings)) AS room,
    (ROW_TO_JSON(suppliers)) AS supplier,
    (ROW_TO_JSON(users)) AS user
FROM procurement_requests
         INNER JOIN procurement_budget_periods ON procurement_budget_periods.id = procurement_requests.budget_period_id
         INNER JOIN (
    SELECT *, current_date > end_date AS is_past
    FROM procurement_budget_periods
) AS procurement_budget_periods_2 ON procurement_budget_periods_2.id = procurement_requests.budget_period_id
         INNER JOIN procurement_categories ON procurement_categories.id = procurement_requests.category_id
         INNER JOIN procurement_main_categories ON procurement_main_categories.id = procurement_categories.main_category_id
         INNER JOIN procurement_organizations ON procurement_organizations.id = procurement_requests.organization_id
         INNER JOIN procurement_organizations AS procurement_departments ON procurement_departments.id = procurement_organizations.parent_id
         INNER JOIN rooms ON rooms.id = procurement_requests.room_id
         INNER JOIN buildings ON buildings.id = rooms.building_id
         LEFT JOIN models ON models.id = procurement_requests.model_id
         LEFT JOIN procurement_templates ON procurement_templates.id = procurement_requests.template_id
         LEFT JOIN suppliers ON suppliers.id = procurement_requests.supplier_id
         LEFT JOIN users ON users.id = procurement_requests.user_id
WHERE
    (procurement_requests.category_id IN (CAST('737f156d-5fa0-485d-8f76-5fac9082d50d' AS UUID)))
  AND (procurement_requests.budget_period_id IN (CAST('7b2680c7-da73-4a68-994d-e20975b8b6dd' AS UUID)))
  AND (procurement_requests.organization_id IN (CAST('0e26f1f0-71b3-44c1-8482-c18270bcd80f' AS UUID)))
  AND (procurement_requests.priority IN ('normal', 'high'))
  AND (procurement_requests.inspector_priority IN ('low', 'medium', 'high', 'mandatory'))
  AND ((procurement_requests.approved_quantity IS NULL) OR (procurement_requests.approved_quantity >= procurement_requests.requested_quantity) OR ((procurement_requests.approved_quantity < procurement_requests.requested_quantity) AND (procurement_requests.approved_quantity > 0)) OR (procurement_requests.approved_quantity = 0))
ORDER BY
    procurement_requests.id ASC,
    concat(
            lower(coalesce(procurement_requests.article_name, '')),
            lower(coalesce(models.product, '')),
            lower(coalesce(models.version, ''))
        ) ASC

