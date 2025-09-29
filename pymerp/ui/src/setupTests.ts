import '@testing-library/jest-dom/vitest';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import fs from 'node:fs';
import Module from 'node:module';

const NodeModule = Module.Module;

const currentDir = path.dirname(fileURLToPath(import.meta.url));
const candidateNodeModulePaths = [
  path.resolve(currentDir, '..', 'node_modules'),
  path.resolve(currentDir, '..', '..', 'node_modules'),
].filter((candidate) => fs.existsSync(candidate));

for (const candidate of candidateNodeModulePaths) {
  if (!NodeModule.globalPaths.includes(candidate)) {
    NodeModule.globalPaths.unshift(candidate);
  }
}

const aliasMap = new Map<string, string>();

function addAlias(request: string, relativePath: string) {
  for (const base of candidateNodeModulePaths) {
    const resolved = path.resolve(base, relativePath);
    if (fs.existsSync(resolved)) {
      aliasMap.set(request, resolved);
      break;
    }
  }
}

addAlias('react', 'react/index.js');
addAlias('react/jsx-runtime', 'react/jsx-runtime.js');
addAlias('react/jsx-dev-runtime', 'react/jsx-dev-runtime.js');
addAlias('react-dom', 'react-dom/index.js');
addAlias('react-dom/client', 'react-dom/client.js');
addAlias('react-dom/server', 'react-dom/server.js');
addAlias('react-dom/test-utils', 'react-dom/test-utils.js');

const originalResolveFilename = NodeModule._resolveFilename;
NodeModule._resolveFilename = function patchedResolveFilename(request, parent, isMain, options) {
  const mapped = aliasMap.get(request);
  if (mapped) {
    return mapped;
  }
  return originalResolveFilename.call(this, request, parent, isMain, options);
};

Object.assign(import.meta.env, {
  VITE_CAPTCHA_ENABLED: import.meta.env.VITE_CAPTCHA_ENABLED ?? 'true',
});
