import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  // Global timeout per test — high because MFA requires manual OTP entry
  timeout: 3 * 60 * 1000, // 3 minutes
  expect: {
    timeout: 15_000,
  },
  fullyParallel: false,
  retries: 0,
  workers: 1,
  reporter: 'list',
  use: {
    // SWAGGER_UI_URL is set by the Maven e2e profile.
    baseURL: process.env.SWAGGER_UI_URL ?? 'http://localhost:8081',
    // Keep browser open so you can see the Okta login screens
    headless: false,
    // Retain trace on failure for debugging
    trace: 'on-first-retry',
    // Allow navigation to external domains (Okta)
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
