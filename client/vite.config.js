import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'
import { nodePolyfills } from 'vite-plugin-node-polyfills'

// https://vitejs.dev/config/
export default defineConfig({
  base: '/',
  plugins: [react(), nodePolyfills()],
  server: {
    port: 4000,
    proxy: {
      '/procure/graphql': 'http://localhost:3230'
    }
  }
})
