import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'node:path'
import fs from 'node:fs'

const resolveModulePath = (moduleSubPath: string) => {
  const localPath = path.resolve(__dirname, 'node_modules', moduleSubPath)

  if (fs.existsSync(localPath)) {
    return localPath
  }

  return path.resolve(__dirname, '..', 'node_modules', moduleSubPath)
}

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      react: resolveModulePath('react'),
      'react/jsx-runtime': resolveModulePath('react/jsx-runtime.js'),
      'react/jsx-dev-runtime': resolveModulePath('react/jsx-dev-runtime.js'),
      'react-dom': resolveModulePath('react-dom'),
      'react-dom/client': resolveModulePath('react-dom/client.js'),
      'react-dom/server': resolveModulePath('react-dom/server.js'),
      'react-dom/test-utils': resolveModulePath('react-dom/test-utils.js'),
    },
    dedupe: ['react', 'react-dom'],
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
  build: {
    chunkSizeWarningLimit: 1024,
    rollupOptions: {
      output: {
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom'],
          charts: ['recharts'],
          table: ['@tanstack/react-table'],
        },
      },
    },
  },
  test: {
    globals: true,
    environment: 'happy-dom',
    setupFiles: './src/setupTests.ts',
    server: {
      deps: {
        inline: true,
      },
    },
  },
})
