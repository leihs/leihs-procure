# My UI

- UI components (React) and theme (based on Bootstrap) for `leihs-admin` app.

## Stack

- React 17
- Bootstrap 4
- Webpack 5
- Babel
- Prettier
- ESLint

## Artifact output paths

- `dist/admin-ui.js`: Component library
- `dist/admin-ui.css`: Theme (styles)

## Guide

### Basics

Modes of development:

- Start UI library in watch mode (`npm run watch`), then start `leihs-admin` app (`../bin/cljs-watch`)  
  Changes in components will automatically reflect in `leihs-my` app. Changes in SCSS require a browser reload.
- Start `test-app` to debug components in a standalone/runnable environment (see below: `npm run start`)

### Lint and format

Note that currently only Prettier's rule definitions are configured in ESLint.

- `npm run lint`: Lint all files
- `npm run prettier`: Autoformat all files

### Library development and build

- `npm run watch`: Start theme and lib in watch/dev mode (use along with watch mode in Admin app)
- `npm run build`: Build theme and lib for production
- `npm run start`: Start dev server for `test-app` (http://localhost:8081/test-app.html)
