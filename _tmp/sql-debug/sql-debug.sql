-- >>> resultA1-2a xxx >query query-bps  [SELECT procurement_main_categories.* FROM procurement_main_categories ORDER BY procurement_main_categories.name ASC]
-- >>> resultA1-2b xxx >query query-bps  [SELECT * FROM procurement_budget_periods WHERE procurement_budget_periods.id IN (CAST(? AS UUID)) ORDER BY end_date DESC 5c1f9a02-a49d-4bbe-976f-e1447bff2ad2]
-- >o> >>> tocheck >query proc-requests [SELECT DISTINCT ON (procurement_requests.id, concat(lower(coalesce(procurement_requests.article_name, '')), lower(coalesce(models.product, '')), lower(coalesce(models.version, '')))) procurement_requests.*, ((CASE WHEN ((procurement_budget_periods.end_date < current_date) AND (procurement_requests.approved_quantity IS NULL)) OR (current_date < procurement_budget_periods.inspection_start_date) THEN ? WHEN (procurement_budget_periods.end_date < current_date) AND (procurement_requests.approved_quantity >= procurement_requests.requested_quantity) THEN ? WHEN (procurement_budget_periods.end_date < current_date) AND ((procurement_requests.approved_quantity < procurement_requests.requested_quantity) AND (procurement_requests.approved_quantity > ?)) THEN ? WHEN (procurement_budget_periods.end_date < current_date) AND (procurement_requests.approved_quantity = ?) THEN ? WHEN (current_date >= procurement_budget_periods.inspection_start_date) AND (current_date < procurement_budget_periods.end_date) THEN ? END)) AS state, (ROW_TO_JSON(procurement_budget_periods_2)) AS budget_period, row_to_json(procurement_categories)::jsonb || jsonb_build_object('main_category', row_to_json(procurement_main_categories)) AS category, (ROW_TO_JSON(models)) AS model, row_to_json(procurement_organizations)::jsonb || jsonb_build_object('department', row_to_json(procurement_departments)) AS organization, (ROW_TO_JSON(procurement_templates)) AS template, row_to_json(rooms)::jsonb || jsonb_build_object('building', row_to_json(buildings)) AS room, (ROW_TO_JSON(suppliers)) AS supplier, (ROW_TO_JSON(users)) AS user FROM procurement_requests INNER JOIN procurement_budget_periods ON procurement_budget_periods.id = procurement_requests.budget_period_id INNER JOIN (SELECT *, current_date > end_date AS is_past FROM procurement_budget_periods) AS procurement_budget_periods_2 ON procurement_budget_periods_2.id = procurement_requests.budget_period_id INNER JOIN procurement_categories ON procurement_categories.id = procurement_requests.category_id INNER JOIN procurement_main_categories ON procurement_main_categories.id = procurement_categories.main_category_id INNER JOIN procurement_organizations ON procurement_organizations.id = procurement_requests.organization_id INNER JOIN procurement_organizations AS procurement_departments ON procurement_departments.id = procurement_organizations.parent_id INNER JOIN rooms ON rooms.id = procurement_requests.room_id INNER JOIN buildings ON buildings.id = rooms.building_id LEFT JOIN models ON models.id = procurement_requests.model_id LEFT JOIN procurement_templates ON procurement_templates.id = procurement_requests.template_id LEFT JOIN suppliers ON suppliers.id = procurement_requests.supplier_id LEFT JOIN users ON users.id = procurement_requests.user_id LEFT JOIN procurement_category_viewers ON procurement_category_viewers.category_id = procurement_requests.category_id WHERE (procurement_requests.category_id IN (CAST(? AS UUID), CAST(? AS UUID), CAST(? AS UUID), CAST(? AS UUID), CAST(? AS UUID), CAST(? AS UUID))) AND (procurement_requests.budget_period_id IN (CAST(? AS UUID))) AND (procurement_requests.priority IN (?)) AND ((procurement_category_viewers.user_id = CAST(? AS UUID)) OR (procurement_requests.user_id = CAST(? AS UUID))) ORDER BY concat(lower(coalesce(procurement_requests.article_name, '')), lower(coalesce(models.product, '')), lower(coalesce(models.version, ''))) ASC NEW APPROVED 0 PARTIALLY_APPROVED 0 DENIED IN_APPROVAL 69e8ac83-e9fc-4efe-aab7-0668104f9de1 86d0b305-49b6-4b3f-92e7-32148f0e7748 5510dbad-671e-4909-a5e9-d9a9168071e9 01d62754-5e3d-4eb8-b2a5-42c286d998fd e5a8f70d-7b3d-413a-8ff4-6718ca83a008 7dfc7f0c-410e-4928-b00b-f2a894e04503 5c1f9a02-a49d-4bbe-976f-e1447bff2ad2 normal #uuid "afee506a-67ca-40f9-9dc3-6dc597d371e6" #uuid "afee506a-67ca-40f9-9dc3-6dc597d371e6"]


SELECT procurement_main_categories.* FROM procurement_main_categories ORDER BY procurement_main_categories.name ASC;
-- 6b824519-a478-4a83-b1f0-e189c4527882,main_category_1
-- 2e751901-b9c0-46a4-86be-66bf3021595e,main_category_2



SELECT * FROM procurement_budget_periods WHERE procurement_budget_periods.id IN (CAST('5c1f9a02-a49d-4bbe-976f-e1447bff2ad2' AS UUID)) ORDER BY end_date DESC;
-- 5c1f9a02-a49d-4bbe-976f-e1447bff2ad2,budget_period_I,2024-01-20 09:36:45.143957 +00:00,2024-03-20 09:36:45.144077 +00:00



-- branch
--           concat(lower(coalesce(procurement_requests.article_name, '')),
--                  lower(coalesce(models.product, '')),
--                  lower(coalesce(models.version, ''))) as concatenated_value,


select
--     a.concatenated_value,
a.article_name,
a.short_id,
a.supplier_name,
a.price_cents,
b.name
from (SELECT DISTINCT ON (procurement_requests.id, concat(lower(coalesce(procurement_requests.article_name, '')),
                                                          lower(coalesce(models.product, '')),
                                                          lower(coalesce(models.version, '')))) procurement_requests.*,
                                                                                                CASE
                                                                                                    WHEN ((procurement_budget_periods.end_date < current_date) AND
                                                                                                          (procurement_requests.approved_quantity IS NULL)) OR
                                                                                                         (current_date < procurement_budget_periods.inspection_start_date)
                                                                                                        THEN 'NEW'
                                                                                                    WHEN (procurement_budget_periods.end_date < current_date) AND
                                                                                                         (procurement_requests.approved_quantity >=
                                                                                                          procurement_requests.requested_quantity)
                                                                                                        THEN 'APPROVED'
                                                                                                    WHEN (procurement_budget_periods.end_date < current_date) AND
                                                                                                         ((procurement_requests.approved_quantity <
                                                                                                           procurement_requests.requested_quantity) AND
                                                                                                          (procurement_requests.approved_quantity > 0))
                                                                                                        THEN 'PARTIALLY_APPROVED'
                                                                                                    WHEN (procurement_budget_periods.end_date < current_date) AND
                                                                                                         (procurement_requests.approved_quantity = 0)
                                                                                                        THEN 'DENIED'
                                                                                                    WHEN (current_date >= procurement_budget_periods.inspection_start_date) AND
                                                                                                         (current_date < procurement_budget_periods.end_date)
                                                                                                        THEN 'IN_APPROVAL'
                                                                                                    END                                                AS state,
                                                                                                ROW_TO_JSON(procurement_budget_periods_2)              AS budget_period,
                                                                                                row_to_json(procurement_categories)::jsonb ||
                                                                                                jsonb_build_object(
                                                                                                        'main_category',
                                                                                                        row_to_json(procurement_main_categories))      AS category,
                                                                                                ROW_TO_JSON(models)                                    AS model,
                                                                                                row_to_json(procurement_organizations)::jsonb ||
                                                                                                jsonb_build_object(
                                                                                                        'department',
                                                                                                        row_to_json(procurement_departments))          AS organization,
                                                                                                ROW_TO_JSON(procurement_templates)                     AS template,
                                                                                                row_to_json(rooms)::jsonb ||
                                                                                                jsonb_build_object('building', row_to_json(buildings)) AS room,
                                                                                                ROW_TO_JSON(suppliers)                                 AS supplier,
                                                                                                ROW_TO_JSON(users)                                     AS user
      FROM procurement_requests
               INNER JOIN procurement_budget_periods
                          ON procurement_budget_periods.id = procurement_requests.budget_period_id
               INNER JOIN (SELECT *, current_date > end_date AS is_past
                           FROM procurement_budget_periods) AS procurement_budget_periods_2
                          ON procurement_budget_periods_2.id = procurement_requests.budget_period_id
               INNER JOIN procurement_categories ON procurement_categories.id = procurement_requests.category_id
               INNER JOIN procurement_main_categories
                          ON procurement_main_categories.id = procurement_categories.main_category_id
               INNER JOIN procurement_organizations
                          ON procurement_organizations.id = procurement_requests.organization_id
               INNER JOIN procurement_organizations AS procurement_departments
                          ON procurement_departments.id = procurement_organizations.parent_id
               INNER JOIN rooms ON rooms.id = procurement_requests.room_id
               INNER JOIN buildings ON buildings.id = rooms.building_id
               LEFT JOIN models ON models.id = procurement_requests.model_id
               LEFT JOIN procurement_templates ON procurement_templates.id = procurement_requests.template_id
               LEFT JOIN suppliers ON suppliers.id = procurement_requests.supplier_id
               LEFT JOIN users ON users.id = procurement_requests.user_id
               LEFT JOIN procurement_category_viewers
                         ON procurement_category_viewers.category_id = procurement_requests.category_id
      WHERE (procurement_requests.category_id IN (
                                                  CAST('0bd626ec-0721-4ed3-99f8-bbc832daedab' AS UUID),
                                                  CAST('5cfad3a9-8250-44d3-9514-9379905c58d9' AS UUID),
                                                  CAST('94c36e2e-1eca-441e-9ba7-3f5355f48458' AS UUID),
                                                  CAST('d4b71427-9ce4-485f-ab18-36454cecbf53' AS UUID),
                                                  CAST('47a92455-38ad-44bb-8a8e-3d006fd0950a' AS UUID),
                                                  CAST('fa5ffd1a-5e5a-40ed-b023-79f882550428' AS UUID)
          ))
        AND (procurement_requests.budget_period_id IN (CAST('a23a82cc-1cd2-4cf8-a2e6-0d9536916276' AS UUID)))
        AND (procurement_requests.priority IN ('normal'))
        AND ((procurement_category_viewers.user_id = CAST('eeef4cf5-82f3-4964-beaa-4bf80a35c2b0' AS UUID)) OR
             (procurement_requests.user_id = CAST('eeef4cf5-82f3-4964-beaa-4bf80a35c2b0' AS UUID)))
      ORDER BY concat(lower(coalesce(procurement_requests.article_name, '')), lower(coalesce(models.product, '')),
                      lower(coalesce(models.version, ''))) ASC) a,
     procurement_budget_periods b
where b.id = a.budget_period_id;

--branch

---------------------l

--master
-- select a.article_name, a.short_id, a.supplier_name, a.price_cents, b.name
-- from (
--
--
--
--       ) a,
--      procurement_budget_periods b
-- where b.id = a.budget_period_id;
--


select a.article_name, a.short_id, a.supplier_name, a.price_cents, b.name
from (

         SELECT DISTINCT ON (procurement_requests.id, concat(lower(coalesce(procurement_requests.article_name, '')), lower(coalesce(models.product, '')), lower(coalesce(models.version, ''))))
             procurement_requests.*,
             CASE
                 WHEN ((procurement_budget_periods.end_date < current_date) AND (procurement_requests.approved_quantity IS NULL)) OR (current_date < procurement_budget_periods.inspection_start_date) THEN 'NEW'
                 WHEN (procurement_budget_periods.end_date < current_date) AND (procurement_requests.approved_quantity >= procurement_requests.requested_quantity) THEN 'APPROVED'
                 WHEN (procurement_budget_periods.end_date < current_date) AND ((procurement_requests.approved_quantity < procurement_requests.requested_quantity) AND (procurement_requests.approved_quantity > 0)) THEN 'PARTIALLY_APPROVED'
                 WHEN (procurement_budget_periods.end_date < current_date) AND (procurement_requests.approved_quantity = 0) THEN 'DENIED'
                 WHEN (current_date >= procurement_budget_periods.inspection_start_date) AND (current_date < procurement_budget_periods.end_date) THEN 'IN_APPROVAL'
                 END AS state,
             ROW_TO_JSON(procurement_budget_periods_2) AS budget_period,
             row_to_json(procurement_categories)::jsonb || jsonb_build_object('main_category', row_to_json(procurement_main_categories)) AS category,
             ROW_TO_JSON(models) AS model,
             row_to_json(procurement_organizations)::jsonb || jsonb_build_object('department', row_to_json(procurement_departments)) AS organization,
             ROW_TO_JSON(procurement_templates) AS template,
             row_to_json(rooms)::jsonb || jsonb_build_object('building', row_to_json(buildings)) AS room,
             ROW_TO_JSON(suppliers) AS supplier,
             ROW_TO_JSON(users) AS user
         FROM
             procurement_requests
                 INNER JOIN procurement_budget_periods ON procurement_budget_periods.id = procurement_requests.budget_period_id
                 INNER JOIN (SELECT *, current_date > end_date AS is_past FROM procurement_budget_periods) AS procurement_budget_periods_2 ON procurement_budget_periods_2.id = procurement_requests.budget_period_id
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
                 LEFT JOIN procurement_category_viewers ON procurement_category_viewers.category_id = procurement_requests.category_id
         WHERE
             (procurement_requests.category_id IN (CAST('69e8ac83-e9fc-4efe-aab7-0668104f9de1' AS UUID), CAST('86d0b305-49b6-4b3f-92e7-32148f0e7748' AS UUID), CAST('5510dbad-671e-4909-a5e9-d9a9168071e9' AS UUID), CAST('01d62754-5e3d-4eb8-b2a5-42c286d998fd' AS UUID), CAST('e5a8f70d-7b3d-413a-8ff4-6718ca83a008' AS UUID), CAST('7dfc7f0c-410e-4928-b00b-f2a894e04503' AS UUID)))
           AND (procurement_requests.budget_period_id IN (CAST('5c1f9a02-a49d-4bbe-976f-e1447bff2ad2' AS UUID)))
           AND (procurement_requests.priority IN ('normal'))
           AND ((procurement_category_viewers.user_id = CAST('afee506a-67ca-40f9-9dc3-6dc597d371e6' AS UUID)) OR (procurement_requests.user_id = CAST('afee506a-67ca-40f9-9dc3-6dc597d371e6' AS UUID)))
         ORDER BY
             concat(lower(coalesce(procurement_requests.article_name, '')), lower(coalesce(models.product, '')), lower(coalesce(models.version, ''))) ASC


      ) a,
     procurement_budget_periods b
where b.id = a.budget_period_id;

-- Anaphoric Macro,             budget_period_I.002,    Gulgowski-Gulgowski,    103,budget_period_I
-- Heavy Duty Silk Table,       budget_period_I.004,    Ritchie-Will,           109,budget_period_I
-- Intelligent Plastic Shoes,   budget_period_I.005,    Schowalter-Homenick,    113,budget_period_I
-- Pandoric Macro,              budget_period_I.003,    Brown-Torp,             107,budget_period_I
-- Practical Silk Plate,        budget_period_I.001,    Pacocha LLC,            101,budget_period_I
