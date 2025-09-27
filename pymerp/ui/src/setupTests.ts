import '@testing-library/jest-dom/vitest';

Object.assign(import.meta.env, {
  VITE_CAPTCHA_ENABLED: import.meta.env.VITE_CAPTCHA_ENABLED ?? 'true',
});
