# Security Vulnerability Fixes

**Date:** 2026-02-19

## Summary

This document describes the security vulnerabilities addressed in this update, along with any remaining items that could not be fully resolved.

---

## Fixed Vulnerabilities

### NPM (client/package-lock.json)

| # | Vulnerability | Package | Severity | Old Version | New Version | CVE |
|---|---|---|---|---|---|---|
| #234 | Prototype Pollution in SheetJS | `xlsx` (direct) | High | 0.18.5 | 0.19.3 (via CDN) | CVE-2023-30533 |
| #268 | SheetJS ReDoS | `xlsx` (direct) | High | 0.18.5 | 0.19.3 (via CDN) | GHSA-5pgg-2g8v-p4x9 |
| #384 | qs arrayLimit bypass (bracket notation) DoS | `qs` | High | 6.11.2 | >=6.14.2 (override) | CVE-2025-15284 |
| #393 | qs arrayLimit bypass (comma parsing) DoS | `qs` | Low | 6.11.2 | >=6.14.2 (override) | CVE-2026-2391 |
| #381 | vite server.fs.deny bypass via backslash on Windows | `vite` (direct) | Moderate | 4.4.9 | ^5.4.21 | CVE-2025-62522 |
| #370 | Vite middleware public directory file serving | `vite` (direct) | Low | 4.4.9 | ^5.4.21 | CVE-2025-58752 |
| #369 | Vite server.fs settings not applied to HTML files | `vite` (direct) | Low | 4.4.9 | ^5.4.21 | CVE-2025-58752 |
| #385 | React Router XSS via Open Redirects | `@remix-run/router` | High | 1.9.0 | >=1.23.2 (via react-router) | CVE-2026-22029 |
| #386 | React Router unexpected external redirect | `react-router` (direct) | Moderate | 6.16.0 | ^6.30.2 | CVE-2025-68470 |
| #388 | Lodash Prototype Pollution in \_.unset and \_.omit | `lodash` (direct) | Moderate | 4.17.11 | ^4.17.23 | CVE-2025-13465 |
| #321 | esbuild dev server allows any website to read responses | `esbuild` | Moderate | 0.18.20 | >=0.25.0 (override) | GHSA-67mh-4wv8-2f99 |

### RubyGems (server/Gemfile.lock)

| # | Vulnerability | Package | Severity | Old Version | New Version | CVE |
|---|---|---|---|---|---|---|
| #394 | Rack Directory Traversal via Rack::Directory | `rack` | High | 3.1.17 | 3.1.20 | - |
| #398 | Stored XSS in Rack::Directory via javascript: filenames | `rack` | Moderate | 3.1.17 | 3.1.20 | CVE-2026-25500 |
| #380 | Rack memory-exhaustion DoS via unbounded URL-encoded body parsing | `rack` | High | 3.1.17 | 3.1.20 | CVE-2025-61771 |
| #379 | Rack possible Information Disclosure | `rack` | Moderate | 3.1.17 | 3.1.20 | CVE-2025-61780 |
| #392 | Faraday SSRF via protocol-relative URL | `faraday` | Moderate | 2.7.1 | 2.14.1 | CVE-2026-25765 |

### RubyGems (Gemfile.lock)

| # | Vulnerability | Package | Severity | Old Version | New Version | CVE |
|---|---|---|---|---|---|---|
| #395 | Rack Directory Traversal via Rack::Directory | `rack` | High | 3.2.4 | 3.2.5 | - |
| #397 | Stored XSS in Rack::Directory via javascript: filenames | `rack` | Moderate | 3.2.4 | 3.2.5 | CVE-2026-25500 |

---

## Remaining / Unresolved Vulnerabilities

| # | Vulnerability | Package | Severity | Reason |
|---|---|---|---|---|
| #396 | ajv ReDoS when using `$data` option | `ajv` (transitive) | Moderate | Transitive dependency from `eslint` 8.x. Fix requires ajv >=8.18.0, but eslint 8.x depends on ajv 6.x. Upgrading eslint to v10+ would resolve this but is a breaking change. Risk is low unless `$data` option is explicitly enabled. |
| #387 | Elliptic risky cryptographic implementation | `elliptic` (transitive) | Low | No fixed version available on npm (latest is 6.6.1). Transitive dependency from `vite-plugin-node-polyfills` -> `node-stdlib-browser` -> `crypto-browserify`. The package is only used for browser polyfills and not for production cryptographic operations. |
| #268 | SheetJS ReDoS (partial) | `xlsx` (direct) | High | Updated to 0.19.3 via SheetJS CDN which fixes prototype pollution. npm audit still flags a ReDoS issue. SheetJS is no longer maintained on npm; version 0.19.3 from the CDN is the latest available community edition. Consider migrating to an alternative like `exceljs` if full resolution is needed. |

---

## Changes Made

### client/package.json

- **xlsx**: `^0.18.5` -> `https://cdn.sheetjs.com/xlsx-0.19.3/xlsx-0.19.3.tgz`
- **vite**: `^4.4.9` -> `^5.4.21`
- **@vitejs/plugin-react-swc**: `^3.3.2` -> `^3.7.0`
- **react-router**: `^6.16.0` -> `^6.30.2`
- **react-router-dom**: `^6.16.0` -> `^6.30.2`
- **lodash**: `^4.17.11` -> `^4.17.23`
- **vitest**: `^0.34.6` -> `^1.6.0` (compatibility with Vite 5)
- **@vitest/coverage-v8**: `^0.34.6` -> `^1.6.0`
- **@vitest/ui**: `^0.34.6` -> `^1.6.0`
- **eslint-plugin-vitest**: `^0.3.2` -> `^0.5.0`
- **eslint-plugin-vitest-globals**: `^1.4.0` -> `^1.5.0`
- Added `overrides` section:
  - `qs`: `>=6.14.2`
  - `esbuild`: `>=0.25.0`

### server/Gemfile.lock

- **rack**: 3.1.17 -> 3.1.20
- **faraday**: 2.7.1 -> 2.14.1
- **faraday-net_http**: 3.0.2 -> 3.4.2
- **rackup**: 2.2.1 -> 2.3.1

### Gemfile.lock (root)

- **rack**: 3.2.4 -> 3.2.5

---

## Potential Breaking Changes

1. **Vite 4 -> 5**: The Vite major version upgrade may require minor config adjustments. The `vite.config.js` syntax is compatible. Key changes in Vite 5:
   - Requires Node.js 18+
   - CJS Node API deprecated
   - `define` values are now used as-is during dev (no longer stringified)
   - See [Vite 5 Migration Guide](https://vite.dev/guide/migration.html)

2. **Vitest 0.34 -> 1.x**: Test configuration may need minor updates. Key changes:
   - Pool options syntax changed
   - Snapshot serialization updated
   - See [Vitest 1.0 Migration](https://vitest.dev/guide/migration.html)

3. **React Router 6.16 -> 6.30**: Generally backward compatible within the 6.x line. However, some stricter validation of redirect paths may cause issues if the app relies on external URL redirects through `navigate()` or `redirect()`.

4. **Faraday 2.7 -> 2.14**: Internal middleware API changes. If custom Faraday middleware is used, review the [Faraday changelog](https://github.com/lostisland/faraday/blob/main/CHANGELOG.md).

---

## Recommendations

1. **Run the full test suite** after these changes to catch any regressions from the major version bumps (especially Vite 5 and Vitest 1.x).
2. **Consider replacing `xlsx`** with `exceljs` or another maintained alternative to fully resolve the SheetJS ReDoS vulnerability.
3. **Plan an ESLint 9+ upgrade** to resolve the transitive `ajv` and `minimatch` vulnerabilities.
4. **Monitor `elliptic`** for a patched release (> 6.6.1). Alternatively, evaluate whether `vite-plugin-node-polyfills` is still needed.
