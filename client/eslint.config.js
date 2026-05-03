const js = require('@eslint/js')
const react = require('eslint-plugin-react')
const vitest = require('eslint-plugin-vitest')
const vitestGlobals = require('eslint-plugin-vitest-globals')
const prettier = require('eslint-config-prettier')
const globals = require('globals')

module.exports = [
  // Base ESLint recommended rules
  js.configs.recommended,

  // Global ignores
  {
    ignores: ['build/**', 'node_modules/**', 'dist/**']
  },

  // Configuration for Node.js scripts
  {
    files: ['scripts/**/*.js', 'src/locale/make-pseudoloc.js', '*.config.js'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'commonjs',
      globals: {
        ...globals.node
      }
    }
  },

  // Main configuration for all JS/JSX files
  {
    files: ['**/*.{js,jsx}'],
    plugins: {
      react,
      vitest
    },
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.browser,
        ...globals.es2021,
        ...vitestGlobals.environments.env.globals,
        // Add Node.js globals for build scripts referenced in source
        process: 'readonly'
      },
      parserOptions: {
        ecmaFeatures: {
          jsx: true
        }
      }
    },
    settings: {
      react: {
        version: 'detect'
      }
    },
    rules: {
      // Spread recommended rules from plugins
      ...react.configs.recommended.rules,
      ...vitest.configs.recommended.rules,

      // Custom overrides
      'no-unused-vars': 'warn',
      'vitest/prefer-to-be': 'off',
      'react/prop-types': 'warn',
      'react/react-in-jsx-scope': 'off' // Not needed in React 17+
    }
  },

  // Prettier config must be last to override formatting rules
  prettier
]
