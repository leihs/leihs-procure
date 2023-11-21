-- 1. FIRST .. ok
-- 2. MISSING procurement_requests.*
-- 3. CASE .. ok

-- =======================

-- THIS SHOULD BE THIRD
SELECT CASE
           WHEN procurement_requests.approved_quantity IS NULL THEN ?
           WHEN procurement_requests.approved_quantity >= procurement_requests.requested_quantity THEN ?
           WHEN (procurement_requests.approved_quantity < procurement_requests.requested_quantity) AND
                (procurement_requests.approved_quantity > $3) THEN ?
           WHEN procurement_requests.approved_quantity = ? THEN ? END AS state,
       (ROW_TO_JSON(procurement_budget_periods_2))                    AS budget_period,
       NULL,
       (ROW_TO_JSON(models))                                          AS model,
       NULL,
       (ROW_TO_JSON(procurement_templates))                           AS template,
       NULL,
       (ROW_TO_JSON(suppliers))                                       AS supplier,
       (ROW_TO_JSON(users))                                           AS user
-- THIS SHOULD BE THIRD END

-- THIS SHOULD BE FIRST
SELECT DISTINCT procurement_requests.id,
                CONCAT(LOWER(COALESCE(procurement_requests.article_name, '')),
                       LOWER(COALESCE(models.product, '')),
                       LOWER(COALESCE(models.version, '')))
-- THIS SHOULD BE FIRST END

FROM procurement_requests
         INNER JOIN procurement_budget_periods
                    ON procurement_budget_periods.id = procurement_requests.budget_period_id
         INNER JOIN (SELECT *, end_date AS is_past FROM procurement_budget_periods) AS procurement_budget_periods_2
                    ON procurement_budget_periods_2.id = procurement_requests.budget_period_id
         INNER JOIN procurement_categories ON procurement_categories.id = procurement_requests.category_id
         INNER JOIN procurement_main_categories
                    ON procurement_main_categories.id = procurement_categories.main_category_id
         INNER JOIN procurement_organizations ON procurement_organizations.id = procurement_requests.organization_id
         INNER JOIN procurement_organizations AS procurement_departments
                    ON procurement_departments.id = procurement_organizations.parent_id
         INNER JOIN rooms ON rooms.id = procurement_requests.room_id
         INNER JOIN buildings ON buildings.id = rooms.building_id
         LEFT JOIN models ON models.id = procurement_requests.model_id
         LEFT JOIN procurement_templates ON procurement_templates.id = procurement_requests.template_id
         LEFT JOIN suppliers ON suppliers.id = procurement_requests.supplier_id
         LEFT JOIN users ON users.id = procurement_requests.user_id

WHERE (procurement_requests.category_id IN (?))
  AND (procurement_requests.budget_period_id IN (?))
  AND (procurement_requests.organization_id IN (?))
  AND (procurement_requests.priority IN (?))
  AND (procurement_requests.inspector_priority IN (?))
  AND ((procurement_requests.approved_quantity IS NULL)
    OR (procurement_requests.approved_quantity >= procurement_requests.requested_quantity)
    OR ((procurement_requests.approved_quantity < procurement_requests.requested_quantity)
        AND (procurement_requests.approved_quantity > ?))
    OR (procurement_requests.approved_quantity = ?))
  AND (procurement_requests.order_status IN
       ((CAST(? AS ORDER_STATUS_ENUM)),
        (CAST(? AS ORDER_STATUS_ENUM)),
        (CAST(? AS ORDER_STATUS_ENUM)),
        (CAST(? AS ORDER_STATUS_ENUM)),
        (CAST(? AS ORDER_STATUS_ENUM))))

ORDER BY procurement_requests.id ASC,
         CONCAT(
                 LOWER(COALESCE(procurement_requests.article_name, ?)),
                 LOWER(COALESCE(models.product, ?)),
                 LOWER(COALESCE(models.version, ?))) ASC,

         procurement_requests.* ASC -- THIS SHOULD BE SECOND
