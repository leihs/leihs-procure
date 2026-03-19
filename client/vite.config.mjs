import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'
import { nodePolyfills } from 'vite-plugin-node-polyfills'
import $monacoEditorPlugin from 'vite-plugin-monaco-editor'

const monacoEditorPlugin = $monacoEditorPlugin.default ?? $monacoEditorPlugin

// https://vitejs.dev/config/
export default defineConfig({
  base: '/procure',
  build: {
    outDir: 'build',
    minify: true,
    sourcemap: false
  },
  plugins: [
    react(),
    nodePolyfills({
      protocolImports: true
    }),
    monacoEditorPlugin({
      languageWorkers: ['editorWorkerService', 'json'],
      customWorkers: [
        {
          label: 'graphql',
          entry: 'monaco-graphql/esm/graphql.worker.js'
        }
      ]
    })
  ],
  server: {
    port: 4000,
    proxy: {
      '/procure/graphql': 'http://localhost:3230',
      '/procure/images': 'http://localhost:3230'
    }
  },
  test: {
    globals: true,
    environment: 'jsdom',
    reporters: ['default', 'html'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html']
    }
  }
})
