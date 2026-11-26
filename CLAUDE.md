# CLAUDE.md — leihs-procure

## Dependency sources (hard rule)

- Install packages **only from the public npm registry** (`registry.npmjs.org`), with normal semver ranges (`"xlsx": "^0.18.5"`).
- **Do not** reference non-npm package sources unless absolutely necessary and explicitly approved by a human. Forbidden without that approval:
  - HTTPS/CDN tarball URLs (e.g. `https://cdn.sheetjs.com/...`)
  - `npm:` aliases to third-party republishes (e.g. `npm:@e965/xlsx@...`)
  - git / GitHub archive URLs as dependency versions
- If the latest fix for a vuln is only available outside npm, **keep the npm package**, document the accepted risk in `.osv-scanner-config.toml`, and do **not** “solve” it by switching to a CDN or mirror.
- Concrete case: **`xlsx` must remain `"xlsx": "^0.18.5"`** (latest on npm). Do not “upgrade” via SheetJS CDN or `@e965/xlsx`.

## Development

See `README.md`. Typical commands:

- Backend: `./bin/dev-run-backend`
- Frontend: `./bin/dev-run-frontend` (under `client/`)
- Format Clojure: `./bin/cljfmt check` / `./bin/cljfmt fix`
- Ruby lint: `./bin/rblint` / `./bin/rblint --fix`
