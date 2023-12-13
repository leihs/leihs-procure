
CURRENT STATE
--
1. Use cases
   1. Login with initial view                      **.. ok**
      1. Setup
         - http://localhost:3230/sign-in?return-to=%2Fprocure
         - Account: `jimmie@goyette.com` 
      2. 2/4 GraphQL-Requests are broken           **.. fixed**
2. Procure tests                                   **.. 8/24 broken**
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


UI-Improvements
--
1. Refresh missing (https://staging.leihs.zhdk.ch/procure/admin/users)
2. Button visual-response e.g. after pushing save-button


Known bugs thrown during tests
--
```log

2023-12-12 11:51:12.212 CET [2580] ERROR:  column "json" does not exist at character 143
2023-12-12 11:51:12.212 CET [2580] STATEMENT:  INSERT INTO procurement_uploads (filename, content_type, size, content, metadata, exiftool_version, exiftool_options) VALUES ($1, $2, $3, $4, json, $5, $6)

2023-12-12 11:56:31.495 CET [2580] ERROR:  syntax error at or near ")" at character 97
2023-12-12 11:56:31.495 CET [2580] STATEMENT:  SELECT procurement_categories.* FROM procurement_categories WHERE procurement_categories.id IN () ORDER BY procurement_categories.name ASC




2023-12-13 19:05:29.018 CET [66552] ERROR:  update or delete on table "procurement_organizations" violates foreign key constraint "fk_rails_c116e35025" on table "procurement_requesters_organizations"
2023-12-13 19:05:29.018 CET [66552] DETAIL:  Key (id)=(303c776f-f8e6-4e39-b45f-faac49eb4ab4) is still referenced from table "procurement_requesters_organizations".
2023-12-13 19:05:29.018 CET [66552] STATEMENT:  DELETE FROM procurement_organizations AS po1 WHERE (po1.parent_id IS NULL) AND NOT EXISTS (SELECT TRUE FROM procurement_organizations AS po2 WHERE po2.parent_id = po1.id)
2023-12-13 19:06:59.712 CET [1359] LOG:  checkpoint starting: time



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