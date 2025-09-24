#!/usr/bin/env node

const { execFileSync, spawnSync } = require('child_process');
const path = require('path');

const args = process.argv.slice(2);

if (args.length === 0) {
  console.error('Usage: node scripts/run-ui.js <npm-arguments...>');
  process.exit(1);
}

const cwd = path.resolve(__dirname, '..', 'ui');
const npmExecPath = process.env.npm_execpath;

try {
  if (npmExecPath) {
    execFileSync(process.execPath, [npmExecPath, ...args], {
      cwd,
      stdio: 'inherit',
    });
  } else {
    const command = process.platform === 'win32' ? 'npm.cmd' : 'npm';
    const result = spawnSync(command, args, {
      cwd,
      stdio: 'inherit',
    });

    if (result.error) {
      throw result.error;
    }

    if (typeof result.status === 'number') {
      process.exit(result.status);
    }
  }
} catch (error) {
  if (typeof error.status === 'number') {
    process.exit(error.status);
  }

  if (error.code === 'ENOENT') {
    console.error('Unable to locate npm. Please ensure Node.js and npm are installed and available on your PATH.');
  } else {
    console.error(error.message);
  }

  process.exit(1);
}
