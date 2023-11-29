
CURRENT STATE
--
1. Use cases
   1. Login with initial view                      **.. broken**
      1. Setup
         - http://localhost:3230/sign-in?return-to=%2Fprocure
         - Account: `jimmie@goyette.com` 
      2. 2/4 GraphQL-Requests are broken 
2. Procure tests                                   **.. broken**
3. Integdration-tests **.. broken**

Causes
--
1. Abandoned helper functions

ToClarify
--
1. `procurement_departments` still needed? dead code
   - FYI: had to create :procurement_departments manually baseed on :procurement_organizations


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