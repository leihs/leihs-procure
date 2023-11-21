-- TOFIX
-- 1) "SELECT DISTINCT ON" instead of "SELECT DISTINCT"
-- 2) jsonb_build_object('department', row_to_json(procurement_departments)) doesn't work, is null

-- 1. FIRST .. ok
-- 2. MISSING procurement_requests.*
-- 3. CASE .. ok

-- =======================
-- THIS SHOULD BE FIRST
-- SELECT DISTINCT procurement_requests.id,

SELECT DISTINCT ON (procurement_requests.id,
    CONCAT(LOWER(COALESCE(procurement_requests.article_name, '')),
           LOWER(COALESCE(models.product, '')),
           LOWER(COALESCE(models.version, '')))

    )
-- THIS SHOULD BE FIRST END
procurement_requests.*,

-- THIS SHOULD BE THIRD
-- SELECT CASE          .. this is wrong _> CASE only
CASE
    WHEN procurement_requests.approved_quantity IS NULL THEN 'NEW'
    WHEN procurement_requests.approved_quantity >= procurement_requests.requested_quantity
        THEN 'APPROVED'
    WHEN (procurement_requests.approved_quantity < procurement_requests.requested_quantity) AND
         (procurement_requests.approved_quantity > 0) THEN 'PARTIALLY_APPROVED'
    WHEN procurement_requests.approved_quantity = 0 THEN 'DENIED'
    END                                                                             AS state,

--                 (ROW_TO_JSON(procurement_budget_periods_2)) AS budget_period,
--                 NULL,             -- category ??                                           -- TODO: FIX THIS, WHY NULL??
--                 (ROW_TO_JSON(models))                       AS model,
--                 NULL,             -- organization ??
--                 (ROW_TO_JSON(procurement_templates))        AS template,
--                 NULL,             -- room
--                 (ROW_TO_JSON(suppliers))                    AS supplier,
--                 (ROW_TO_JSON(users))                        AS user

-- FYI: REPLACEMENT BASED ON MASTER-VERSION
row_to_json(models)                                                                 AS model,
row_to_json(procurement_organizations)::jsonb ||
jsonb_build_object('department', row_to_json(procurement_departments))              AS organization,
row_to_json(procurement_templates)                                                  AS template,
row_to_json(rooms)::jsonb || jsonb_build_object('building', row_to_json(buildings)) AS room,
row_to_json(suppliers)                                                              AS supplier,
row_to_json(users)                                                                  AS user
-- THIS SHOULD BE THIRD END


FROM procurement_requests
         INNER JOIN procurement_budget_periods ON procurement_budget_periods.id = procurement_requests.budget_period_id

         INNER JOIN (SELECT *, current_date > end_date AS is_past
--                      FROM procurement_budget_periods) AS procurement_budget_periods_2  -- TODO: FIX THIS
                     FROM procurement_budget_periods) procurement_budget_periods_2
                    ON procurement_budget_periods_2.id = procurement_requests.budget_period_id

         INNER JOIN procurement_categories ON procurement_categories.id = procurement_requests.category_id
         INNER JOIN procurement_main_categories
                    ON procurement_main_categories.id = procurement_categories.main_category_id
         INNER JOIN procurement_organizations ON procurement_organizations.id = procurement_requests.organization_id

    --          INNER JOIN procurement_organizations AS procurement_departments        -- TODO: FIX THIS
         INNER JOIN procurement_organizations procurement_departments
                    ON procurement_departments.id = procurement_organizations.parent_id

         INNER JOIN rooms ON rooms.id = procurement_requests.room_id
         INNER JOIN buildings ON buildings.id = rooms.building_id
         LEFT JOIN models ON models.id = procurement_requests.model_id
         LEFT JOIN procurement_templates ON procurement_templates.id = procurement_requests.template_id
         LEFT JOIN suppliers ON suppliers.id = procurement_requests.supplier_id
         LEFT JOIN users ON users.id = procurement_requests.user_id

-- WHERE (
--                   procurement_requests.category_id in ('7d5ba731-edd9-41ba-8773-7337d24c2327')
--               AND procurement_requests.budget_period_id in ('8b8fe440-cae5-4bf9-8048-d0ec2399faa1')
--               AND procurement_requests.organization_id in ('fb664326-a8ef-4556-af02-07d3127cd9ec')
--               AND (procurement_requests.priority IN ('normal'))
--               AND (procurement_requests.inspector_priority IN ('high'))
--               AND (
--                           (procurement_requests.approved_quantity IS NULL)
--                           OR (procurement_requests.approved_quantity >= procurement_requests.requested_quantity)
--                           OR ((procurement_requests.approved_quantity < procurement_requests.requested_quantity) AND
--                               (procurement_requests.approved_quantity > 0))
--                           OR (procurement_requests.approved_quantity = 0)
--                       )
--               AND (procurement_requests.order_status IN (
-- --        (CAST(? AS ORDER_STATUS_ENUM)),
-- --         (CAST(? AS ORDER_STATUS_ENUM)),
-- --         (CAST(? AS ORDER_STATUS_ENUM)),
-- --         (CAST(? AS ORDER_STATUS_ENUM)),
-- --         (CAST(? AS ORDER_STATUS_ENUM)))
--
--                                                          CAST('not_processed' AS order_status_enum),
--                                                          CAST('in_progress' AS order_status_enum),
--                                                          CAST('procured' AS order_status_enum),
--                                                          CAST('alternative_procured' AS order_status_enum),
--                                                          CAST('not_procured' AS order_status_enum)
--               )))

-- ORDER BY procurement_requests.id ASC,            .. this is wrong _> CASE only
ORDER BY CONCAT(
                 LOWER(COALESCE(procurement_requests.article_name, '')),
                 LOWER(COALESCE(models.product, '')),
                 LOWER(COALESCE(models.version, ''))) ASC

