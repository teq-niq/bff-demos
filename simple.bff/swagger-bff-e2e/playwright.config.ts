import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  // No TOTP pause needed — form login is fully automated
  timeout: 60 * 1000, // 60 seconds per test
  expect: {
    timeout: 15_000,
  },
  fullyParallel: false,
  retries: 0,
  workers: 1,
  reporter: 'list',
  use: {
    // SWAGGER_UI_URL is set by the Maven e2e profile.
    baseURL: process.env.SWAGGER_UI_URL ?? 'http://localhost:8080',
    // Keep browser open so you can observe the login screens
    headless: false,
    // Retain trace on failure for debugging
    trace: 'on-first-retry',
    navigationTimeout: 30_000,
    actionTimeout: 15_000,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
