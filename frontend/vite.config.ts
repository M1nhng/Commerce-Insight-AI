import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  resolve: {
    alias: {
      // @/ maps to src/ — matching tsconfig paths
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    rollupOptions: {
      output: {
        // Sprint 14: split the two heaviest vendor groups out of the entry /
        // dashboard chunk so no single file trips Vite's 500 kB warning.
        // Charts (recharts + its d3 transitive deps) are only needed by the
        // analytics dashboard; react/router are shared by every route.
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          if (
            /[\\/]node_modules[\\/](recharts|d3-[^\\/]+|victory-vendor|internmap|decimal\.js-light)[\\/]/.test(id)
          ) {
            return 'charts-vendor'
          }
          if (/[\\/]node_modules[\\/](react|react-dom|react-router|react-router-dom|scheduler)[\\/]/.test(id)) {
            return 'react-vendor'
          }
        },
      },
    },
  },
  server: {
    port: 5174,
    // Proxy API calls to Spring Boot in dev
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
