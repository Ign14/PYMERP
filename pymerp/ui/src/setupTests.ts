import '@testing-library/jest-dom/vitest'
import { fileURLToPath } from 'node:url'
import path from 'node:path'
import fs from 'node:fs'
import module from 'node:module'

const currentDir = path.dirname(fileURLToPath(import.meta.url))
const projectNodeModules = path.resolve(currentDir, '..', 'node_modules')
const workspaceNodeModules = path.resolve(currentDir, '..', '..', 'node_modules')

// Add both node_modules paths to global paths
for (const candidate of [workspaceNodeModules, projectNodeModules]) {
  if (fs.existsSync(candidate) && !module.Module.globalPaths.includes(candidate)) {
    module.Module.globalPaths.unshift(candidate)
  }
}

// Create alias map for React modules
const aliasMap = new Map<string, string>([
  ['react', path.resolve(workspaceNodeModules, 'react/index.js')],
  ['react/jsx-runtime', path.resolve(workspaceNodeModules, 'react/jsx-runtime.js')],
  ['react/jsx-dev-runtime', path.resolve(workspaceNodeModules, 'react/jsx-dev-runtime.js')],
  ['react-dom', path.resolve(workspaceNodeModules, 'react-dom/index.js')],
  ['react-dom/client', path.resolve(workspaceNodeModules, 'react-dom/client.js')],
  ['react-dom/server', path.resolve(workspaceNodeModules, 'react-dom/server.js')],
  ['react-dom/test-utils', path.resolve(workspaceNodeModules, 'react-dom/test-utils.js')],
])

// Patch module resolution
const originalResolveFilename = module.Module._resolveFilename
module.Module._resolveFilename = function patchedResolveFilename(request, parent, isMain, options) {
  const mapped = aliasMap.get(request)
  if (mapped) {
    return mapped
  }
  return originalResolveFilename.call(this, request, parent, isMain, options)
}

// Set environment variables for tests
Object.assign(import.meta.env, {
  VITE_CAPTCHA_ENABLED: import.meta.env.VITE_CAPTCHA_ENABLED ?? 'true',
})
