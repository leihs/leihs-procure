
CURRENT STATE
--
1. Use cases
   1. Login with initial view                      **.. ok**
      1. Setup
         - http://localhost:3230/sign-in?return-to=%2Fprocure
         - Account: `jimmie@goyette.com` 
      2. 2/4 GraphQL-Requests are broken           **.. fixed**
2. Procure tests                                   **.. 13/24 broken**
3. Integration-tests                               **.. broken**

Causes
--
1. Abandoned helper functions
   1. `[[]]` instead of `sql/call` 
2. Replacement of `"~~*"` by honey-support by `:ilike`
3. Buggy refactoring of auth/permission
4. Not support `jdbc-next/:row-fn` (no errors or exceptions)


Current focus to fix:
1. ApolloError: ERROR: non-integer constant in ORDER BY
   Position: 190
2. ApolloError: Unable to serialize #inst "2024-01-03T23:00:00.000-00:00" as type 'Boolean'.
   Field resolver returned an undefined enum value.
3. ApolloError: Field resolver returned an undefined enum value.
   Field resolver returned an undefined enum value.



ToClarify
--
1. `procurement_departments` still needed? dead code
   - FYI: had to create :procurement_departments manually based on :procurement_organizations


Known bugs
--
```log
FIXED ERROR:  non-integer constant in ORDER BY at character 190
2023-12-04 09:24:11.796 CET [4795] STATEMENT:  SELECT procurement_templates.* FROM procurement_templates LEFT JOIN models ON models.id = procurement_templates.model_id WHERE procurement_templates.category_id = CAST($1 AS UUID) ORDER BY NULL ASC

FIXED ERROR:  non-integer constant in ORDER BY at character 131
2023-12-04 09:17:48.239 CET [4795] STATEMENT:  SELECT procurement_templates.* FROM procurement_templates LEFT JOIN models ON models.id = procurement_templates.model_id ORDER BY NULL ASC



:( [ { "message": "Key must be integer", "locations": [ { "line": 2, "column": 3 } ], "path": [ "users" ], "extensions": { "exception": "IllegalArgumentException", "arguments": { "search_term": "$searchTerm", "limit": 25, "offset": 0, "exclude_ids": "$excludeIds", "isRequester": "$isRequester" } } } ]
Abrechnungsart *
Kostenstelle
Sachkonto I (ER)
Beschaffungs-Status *
Beschaffungs-Kommentar


>e> #error {
 :cause ERROR: new row for relation "procurement_templates" violates check constraint "check_either_model_id_or_article_name"
  Detail: Failing row contains (acbdf40f-aad4-4be9-a364-e7974e26c938, null, null, null, null, 0, CHF, null, e2b9cf5b-4803-48ac-98bb-8f70997a53ab, f).
 :via
 [{:type org.postgresql.util.PSQLException
   :message ERROR: new row for relation "procurement_templates" violates check constraint "check_either_model_id_or_article_name"
  Detail: Failing row contains (acbdf40f-aad4-4be9-a364-e7974e26c938, null, null, null, null, 0, CHF, null, e2b9cf5b-4803-48ac-98bb-8f70997a53ab, f).
   :at [org.postgresql.core.v3.QueryExecutorImpl receiveErrorResponse QueryExecutorImpl.java 2533]}]
 :trace
 [[org.postgresql.core.v3.QueryExecutorImpl receiveErrorResponse QueryExecutorImpl.java 2533]


ERROR:  syntax error at or near "uuid" at character 81
2023-12-01 19:21:41.922 CET [87369] STATEMENT:  SELECT * FROM procurement_budget_periods WHERE procurement_budget_periods.id IN uuid ORDER BY end_date DESC

 ERROR:  argument of OR must be type boolean, not type record at character 4044

SELECT DISTINCT ON (procurement_requests.id, concat(lower(coalesce(procurement_requests.article_name, '')), lower(coalesce(models.product, '')), lower(coalesce(models.version, '')))) procurement_requests.*, ((CASE WHEN ((procurement_budget_periods.end_date < current_date) AND (procurement_requests.approved_quantity IS NULL)) OR (current_date < procurement_budget_periods.inspection_start_date) THEN $1 WHEN (procurement_budget_periods.end_date < current_date) AND (procurement_requests.approved_quantity >= procurement_requests.requested_quantity) THEN $2 WHEN (procurement_budget_periods.end_date < current_date) AND ((procurement_requests.approved_quantity < procurement_requests.requested_quantity) AND (procurement_requests.approved_quantity > $3)) THEN $4 WHEN (procurement_budget_periods.end_date < current_date) AND (procurement_requests.approved_quantity = $5) THEN $6 WHEN (current_date >= procurement_budget_periods.inspection_start_date) AND (current_date < procurement_budget_periods.end_date) THEN $7 END)) AS state, (ROW_TO_JSON(procurement_budget_periods_2)) AS budget_period, row_to_json(procurement_categories)::jsonb || jsonb_build_object('main_category', row_to_json(procurement_main_categories)) AS category, (ROW_TO_JSON(models)) AS model, row_to_json(procurement_organizations)::jsonb || jsonb_build_object('department', row_to_json(procurement_departments)) AS organization, (ROW_TO_JSON(procurement_templates)) AS template, row_to_json(rooms)::jsonb || jsonb_build_object('building', row_to_json(buildings)) AS room, (ROW_TO_JSON(suppliers)) AS supplier, (ROW_TO_JSON(users)) AS user FROM procurement_requests INNER JOIN procurement_budget_periods ON procurement_budget_periods.id = procurement_requests.budget_period_id INNER JOIN (SELECT *, current_date > end_date AS is_past FROM procurement_budget_periods) AS procurement_budget_periods_2 ON procurement_budget_periods_2.id = procurement_requests.budget_period_id INNER JOIN procurement_categories ON procurement_categories.id = procurement_requests.category_id INNER JOIN procurement_main_categories ON procurement_main_categories.id = procurement_categories.main_category_id INNER JOIN procurement_organizations ON procurement_organizations.id = procurement_requests.organization_id INNER JOIN procurement_organizations AS procurement_departments ON procurement_departments.id = procurement_organizations.parent_id INNER JOIN rooms ON rooms.id = procurement_requests.room_id INNER JOIN buildings ON buildings.id = rooms.building_id LEFT JOIN models ON models.id = procurement_requests.model_id LEFT JOIN procurement_templates ON procurement_templates.id = procurement_requests.template_id LEFT JOIN suppliers ON suppliers.id = procurement_requests.supplier_id LEFT JOIN users ON users.id = procurement_requests.user_id LEFT JOIN procurement_category_viewers ON procurement_category_viewers.category_id = procurement_requests.category_id WHERE (TRUE = FALSE) AND (procurement_requests.budget_period_id IN (CAST($8 AS UUID))) AND (procurement_requests.organization_id IN (CAST($9 AS UUID))) AND (procurement_requests.priority IN ($10, $11)) AND (procurement_requests.inspector_priority IN ($12, $13, $14)) AND ((((procurement_budget_periods.end_date < current_date) AND (procurement_requests.approved_quantity IS NULL)) OR (current_date < procurement_budget_periods.inspection_start_date)) OR ((procurement_budget_periods.end_date < current_date) AND (procurement_requests.approved_quantity >= procurement_requests.requested_quantity)) OR ((procurement_budget_periods.end_date < current_date) AND ((procurement_requests.approved_quantity < procurement_requests.requested_quantity) AND (procurement_requests.approved_quantity > $15))) OR ((procurement_budget_periods.end_date < current_date) AND (procurement_requests.approved_quantity = $16))) AND (procurement_requests.order_status IN ((CAST($17 AS ORDER_STATUS_ENUM)), (CAST($18 AS ORDER_STATUS_ENUM)), (CAST($19 AS ORDER_STATUS_ENUM)), (CAST($20 AS ORDER_STATUS_ENUM)), (CAST($21 AS ORDER_STATUS_ENUM)))) AND 

(($22, buildings.name, $23) OR ($24, procurement_requests.short_id, $25) OR ($26, procurement_requests.article_name, $27) OR ($28, procurement_requests.article_number, $29) OR 

($30, procurement_requests.inspection_comment, $31) OR ($32, procurement_requests.order_comment, $33) OR ($34, procurement_requests.motivation, $35) OR ($36, procurement_requests.receiver, $37) OR ($38, procurement_requests.supplier_name, $39) OR ($40, rooms.name, $41) OR ($42, models.product, $43) OR ($44, models.version, $45) OR ($46, users.firstname, $47) OR ($48, users.lastname, $49)) AND ((procurement_category_viewers.user_id = $50) OR (procurement_requests.user_id = $51)) ORDER BY concat(lower(coalesce(procurement_requests.article_name, '')), lower(coalesce(models.product, '')), lower(coalesce(models.version, ''))) ASC


[46953] ERROR:  column "room_id" is of type uuid but expression is of type character varying at character 246
[46953] HINT:  You will need to rewrite or cast the expression.
[46953] STATEMENT:  UPDATE procurement_requests SET motivation = $1, article_number = $2, price_cents = $3, article_name = $4, replacement = TRUE, supplier_name = $5, order_status = (CAST($6 AS ORDER_STATUS_ENUM)), requested_quantity = $7, priority = $8, room_id = $9, receiver = $10 WHERE procurement_requests.id = CAST($11 AS UUID)
 
[46953] ERROR:  syntax error at or near ")" at character 97
[46953] STATEMENT:  SELECT procurement_categories.* FROM procurement_categories WHERE procurement_categories.id IN () ORDER BY procurement_categories.name ASC

[46953] ERROR:  column "category_id" is of type uuid but expression is of type character varying at character 90
[46953] HINT:  You will need to rewrite or cast the expression.
[46953] STATEMENT:  INSERT INTO procurement_templates (article_name, category_id, supplier_name) VALUES ($1, $2, NULL)









 ERROR:  SELECT DISTINCT ON expressions must match initial ORDER BY expressions at character 21
2023-12-09 06:33:53.529 CET [71438] STATEMENT:  SELECT DISTINCT ON (procurement_requests.id, concat(lower(coalesce(procurement_requests.article_name, '')), lower(coalesce(models.product, '')), lower(coalesce(models.version, '')))) procurement_requests.*, ((CASE WHEN procurement_requests.approved_quantity IS NULL THEN $1 WHEN procurement_requests.approved_quantity >= procurement_requests.requested_quantity THEN $2 WHEN (procurement_requests.approved_quantity < procurement_requests.requested_quantity) AND (procurement_requests.approved_quantity > $3) THEN $4 WHEN procurement_requests.approved_quantity = $5 THEN $6 END)) AS state, procurement_requests.*, ((CASE WHEN procurement_requests.approved_quantity IS NULL THEN $7 WHEN procurement_requests.approved_quantity >= procurement_requests.requested_quantity THEN $8 WHEN (procurement_requests.approved_quantity < procurement_requests.requested_quantity) AND (procurement_requests.approved_quantity > $9) THEN $10 WHEN procurement_requests.approved_quantity = $11 THEN $12 END)) AS state FROM procurement_requests INNER JOIN procurement_budget_periods ON procurement_budget_periods.id = procurement_requests.budget_period_id LEFT JOIN models ON models.id = procurement_requests.model_id ORDER BY concat(lower(coalesce(procurement_requests.article_name, '')), lower(coalesce(models.product, '')), lower(coalesce(models.version, ''))) ASC, created_at DESC LIMIT $13


```


```
files=3179, longest=0.022 s, average=0.001 s; distance=2574 kB, estimate=3789 kB
2023-12-08 18:16:57.757 CET [43260] ERROR:  syntax error at or near ")" at character 97
2023-12-08 18:16:57.757 CET [43260] STATEMENT:  SELECT procurement_categories.* FROM procurement_categories WHERE procurement_categories.id IN () ORDER BY procurement_categories.name ASC
2023-12-08 18:17:04.241 CET [43262] ERROR:  new row for relation "procurement_templates" violates check constraint "check_either_model_id_or_article_name"
2023-12-08 18:17:04.241 CET [43262] DETAIL:  Failing row contains (a7b7eb30-39b4-4122-8a00-de0b2a9d2d76, null, null, null, null, 0, CHF, null, c0e853e2-3420-4dc7-8428-a2f75cd2e9ed, f).
2023-12-08 18:17:04.241 CET [43262] STATEMENT:  INSERT INTO procurement_templates (category_id, supplier_name) VALUES ((CAST($1 AS UUID)), NULL)
2023-12-08 18:17:11.893 CET [43262] ERROR:  column "id" is of type uuid but expression is of type character varying at character 39
2023-12-08 18:17:11.893 CET [43262] HINT:  You will need to rewrite or cast the expression.
2023-12-08 18:17:11.893 CET [43262] STATEMENT:  UPDATE procurement_templates SET id = $1, article_name = $2, article_number = $3, price_cents = $4, is_archived = TRUE, category_id = $5, supplier_name = $6 WHERE procurement_templates.id = CAST($7 AS UUID)
2023-12-08 18:17:19.082 CET [43262] ERROR:  column "id" is of type uuid but expression is of type character varying at character 39
2023-12-08 18:17:19.082 CET [43262] HINT:  You will need to rewrite or cast the expression.
2023-12-08 18:17:19.082 CET [43262] STATEMENT:  UPDATE procurement_templates SET id = $1, article_name = $2, article_number = $3, price_cents = $4, is_archived = TRUE, category_id = $5, supplier_name = $6 WHERE procurement_templates.id = CAST($7 AS UUID)
2023-12-08 18:17:25.442 CET [59156] ERROR:  column "id" is of type uuid but expression is of type character varying at character 39
2023-12-08 18:17:25.442 CET [59156] HINT:  You will need to rewrite or cast the expression.
2023-12-08 18:17:25.442 CET [59156] STATEMENT:  UPDATE procurement_templates SET id = $1, article_name = $2, article_number = $3, price_cents = $4, is_archived = FALSE, category_id = $5, supplier_name = $6 WHERE procurement_templates.id = CAST($7 AS UUID)
2023-12-08 18:17:32.426 CET [59156] ERROR:  operator does not exist: uuid = character varying at character 93
2023-12-08 18:17:32.426 CET [59156] HINT:  No operator matches the given name and argument types. You might need to add explicit type casts.
2023-12-08 18:17:32.426 CET [59156] STATEMENT:  SELECT procurement_categories.* FROM procurement_categories WHERE procurement_categories.id IN ($1, $2, $3, $4, $5) ORDER BY procurement_categories.name ASC
2023-12-08 18:17:39.018 CET [59156] ERROR:  column "id" is of type uuid but expression is of type character varying at character 39
2023-12-08 18:17:39.018 CET [59156] HINT:  You will need to rewrite or cast the expression.
2023-12-08 18:17:39.018 CET [59156] STATEMENT:  UPDATE procurement_templates SET id = $1, article_name = $2, article_number = $3, price_cents = $4, is_archived = FALSE, category_id = $5, supplier_name = $6 WHERE procurement_templates.id = CAST($7 AS UUID)
2023-12-08 18:17:40.304 CET [958] LOG:  checkpoint starting: time
2023-12-08 18:17:45.658 CET [59156] ERROR:  column "id" is of type uuid but expression is of type character varying at character 39
2023-12-08 18:17:45.658 CET [59156] HINT:  You will need to rewrite or cast the expression.
2023-12-08 18:17:45.658 CET [59156] STATEMENT:  UPDATE procurement_templates SET id = $1, article_name = $2, article_number = $3, price_cents = $4, is_archived = FALSE, category_id = $5, supplier_name = $6 WHERE procurement_templates.id = CAST($7 AS UUID)
```



TODO Procure
--
1. Null-Value: join-and-nest-categories  join-and-nest-organizations  join-and-nest-rooms       
   - https://github.com/leihs/leihs-procure/pull/124/files#diff-42ad61865b29c0364bf55152fe782f33ee8afe47d684b49778dba78c0497474bR22-R38
2. Not integrated?  join-and-nest-budget-periods
   - https://github.com/leihs/leihs-procure/pull/124/files#diff-42ad61865b29c0364bf55152fe782f33ee8afe47d684b49778dba78c0497474bR59-R69
3. AS-Issue
   - https://github.com/leihs/leihs-procure/pull/124/files#diff-cfe4fe376840d73c0e33b8d8a383945e5dc26e9e8bd0c7a129dd01f326487f8eR58
   - https://github.com/leihs/leihs-procure/pull/124/files#diff-42ad61865b29c0364bf55152fe782f33ee8afe47d684b49778dba78c0497474bR65-R66
4. AS-Issue
   - https://github.com/leihs/leihs-procure/pull/124/files#diff-42ad61865b29c0364bf55152fe782f33ee8afe47d684b49778dba78c0497474bR55-R57
   - https://github.com/leihs/leihs-procure/pull/124/files#diff-cfe4fe376840d73c0e33b8d8a383945e5dc26e9e8bd0c7a129dd01f326487f8eR67
5. SELECT DISTINCT ON / wrong order
   - https://github.com/leihs/leihs-procure/blob/f7c63e34cc613b7e3f4e5df606989c182cbb39ef/server/src/leihs/procurement/resources/request.clj#L190