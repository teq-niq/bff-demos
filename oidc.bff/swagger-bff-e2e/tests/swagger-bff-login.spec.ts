/**
 * E2E test: Swagger UI BFF login via Okta redirect (spring.oidc.bff, port 8081)
 *
 * Flow:
 *   1. Open Swagger UI  →  click Authorize
 *   2. BFF extension auto-redirects to Okta (redirectforlogin: true, 50 ms timer)
 *   3. Okta — enter username  →  Next
 *   4. Okta — method selection screen  →  select Okta Verify FastPass
 *   5. Approve the FastPass prompt on the enrolled device
 *   6. If Okta requests a password, enter it
 *   7. Redirected back to Swagger UI; BffEnabler checks /shortprofile and locks the padlock
 *   8. Assert padlock is locked + /shortprofile returns { loggedIn: true }
 *   9. Assert /secured/user returns 200 (user@example.com has ROLE_myuser)
 *  10. Navigate to /apilogout?source=swagger — BFF clears session, Okta logout, back to Swagger UI
 *  11. Assert padlock is unlocked
 *  12. Assert /shortprofile returns { loggedIn: false }
 *  13. Assert /secured/user returns 401 (session gone)
 *
 * Prerequisites:
 *   - BFF app running:  cd bff && mvn -pl bff-spring-projs/spring.oidc.bff spring-boot:run ...
 *   - Okta tenant live — pass via -Dokta.tenant.id
 *   - Test user credentials — pass via -Dokta.test.user.email and -Dokta.test.user.password
 *   - User must have logged in manually at least once (Okta Verify enrolled,
 *     no first-time setup screens expected)
 *
 * Run via Maven (Swagger UI served from the BFF, port 8081):
 *   mvn -pl bff-spring-projs/spring.oidc.bff verify -Pe2e \
 *       -Dokta.tenant.id=X -Dokta.oauth2.client-id=Y -Dokta.oauth2.client-secret=Z
 *
 * Run directly (app already running, env vars set manually):
 *   cd bff-spring-projs/spring.oidc.bff/swagger-bff-e2e
 *   $env:SWAGGER_UI_URL="http://localhost:8081"
 *   npx playwright test --headed
 */

import { test, expect, type Page } from '@playwright/test';

async function showInstructionBanner(page: Page, message: string) {
  await page.evaluate((text) => {
    document.getElementById('e2e-fastpass-instructions')?.remove();
    const banner = document.createElement('div');
    banner.id = 'e2e-fastpass-instructions';
    banner.setAttribute('role', 'status');
    banner.textContent = text;
    banner.style.cssText = [
      'position:fixed', 'top:0', 'left:0', 'width:100%',
      'padding:18px', 'background:#fff3cd', 'color:#332701',
      'font-size:18px', 'font-weight:bold', 'z-index:99999', 'text-align:center',
      'box-sizing:border-box', 'pointer-events:none',
    ].join(';');
    document.body.appendChild(banner);
  }, message);
}

// ---------------------------------------------------------------------------
// Credentials / config
// SWAGGER_UI_URL — the single app origin used for Swagger UI and BFF calls.
// Injected by the Maven e2e profile.
// ---------------------------------------------------------------------------
const APP_ORIGIN = process.env.SWAGGER_UI_URL ?? 'http://localhost:8081';
const APP_URL = `${APP_ORIGIN}/swagger-ui/index.html`;
const TEST_USER_EMAIL    = process.env.TEST_USER_EMAIL    ?? '';
const TEST_USER_PASSWORD = process.env.TEST_USER_PASSWORD ?? '';
const OKTA_TENANT_ID     = process.env.OKTA_TENANT_ID     ?? '';
const OKTA_DOMAIN        = new RegExp(`${OKTA_TENANT_ID}\.okta\.com`);

if (!TEST_USER_EMAIL || !TEST_USER_PASSWORD || !OKTA_TENANT_ID) {
  throw new Error(
    'Missing required env vars: TEST_USER_EMAIL, TEST_USER_PASSWORD, OKTA_TENANT_ID.\n' +
    'Pass them via Maven -D flags or set them in your shell before running Playwright directly.'
  );
}

// ---------------------------------------------------------------------------
// Test
// ---------------------------------------------------------------------------
test('Swagger UI BFF login — redirect flow with Okta Verify FastPass', async ({ page }) => {

  // ------------------------------------------------------------------
  // 1. Open Swagger UI
  // ------------------------------------------------------------------
  await page.goto(APP_URL);
  // Wait for SwaggerUI bundle to finish rendering
  await page.waitForSelector('.swagger-ui .info', { timeout: 15_000 });

  // ------------------------------------------------------------------
  // 2. Click the top-level "Authorize" button
  //    The BFF http-auth.jsx component mounts, detects redirectforlogin=true,
  //    and fires window.location.href after 50 ms.
  // ------------------------------------------------------------------
  await page.click('button.btn.authorize');

  // ------------------------------------------------------------------
  // 3. Wait for redirect to Okta
  // ------------------------------------------------------------------
  await page.waitForURL(OKTA_DOMAIN, { timeout: 15_000 });

  // ------------------------------------------------------------------
  // 4. Okta — username screen
  // ------------------------------------------------------------------
  await page.waitForSelector('input[name="identifier"]', { timeout: 15_000 });
  await page.fill('input[name="identifier"]', TEST_USER_EMAIL);
  await page.click('[data-se="o-form-input-submit"], input[type="submit"]');

  // ------------------------------------------------------------------
  // 5. Okta — select the FastPass option shown on the method screen.
  // ------------------------------------------------------------------
  const fastPassOption = page.getByRole('link', { name: 'Select Okta Verify.' });
  const initialPassword = page.getByRole('textbox', { name: 'Password', exact: true });
  await expect(fastPassOption.or(initialPassword).first()).toBeVisible({ timeout: 15_000 });
  if (await initialPassword.isVisible()) {
    await initialPassword.fill(TEST_USER_PASSWORD);
    await page.getByRole('button', { name: 'Verify', exact: true }).click();
  }
  const setupOktaVerify = page.getByRole('link', { name: 'Set up Okta Verify.', exact: true });
  await expect(fastPassOption.or(setupOktaVerify).first()).toBeVisible({ timeout: 15_000 });
  if (await setupOktaVerify.isVisible()) {
    await showInstructionBanner(page,
      'TEST: SETUP REQUIRED - This is as far as the automated test can go. ' +
      'Complete the setup steps manually, then rerun E2E. ' +
      'Setup may open another browser outside Playwright control.');
    console.log('SETUP REQUIRED: Complete Okta Verify setup manually outside Playwright, then rerun E2E.');
    await page.waitForTimeout(15_000);
    throw new Error('Okta Verify enrollment required. Complete setup manually outside Playwright, then rerun E2E.');
  }
  await expect(fastPassOption).toBeVisible({ timeout: 15_000 });
  console.log([
    '',
    '======================================================================',
    'ACTION REQUIRED: Complete Okta Verify FastPass',
    'If the browser asks to access apps/services on this device, click Allow.',
    'On the browser application prompt, click Open Okta Verify.',
    'Complete any approval requested in the Windows Okta Verify app.',
    'No TOTP code is needed. The test continues automatically after sign-in.',
    'The test will wait up to 150 seconds after selecting FastPass.',
    '======================================================================',
    '',
  ].join('\n'));
  await showInstructionBanner(page,
      'TEST: Action required - Click Allow if the browser asks to access apps/services, ' +
      'then click Open Okta Verify. Complete approval in the Windows Okta Verify app. ' +
      'No TOTP code is needed. The test continues automatically; you have up to 150 seconds.');
  await fastPassOption.click();

  // ------------------------------------------------------------------
  // 6. Wait for FastPass approval to return to Swagger or for Okta to
  //    request a password. Complete any device prompt in the browser/app.
  // ------------------------------------------------------------------
  await page.waitForFunction((appOrigin) => {
    const app = new URL(appOrigin);
    const returnedToSwagger = location.origin === app.origin && location.pathname.includes('/swagger-ui');
    const passwordRequested = document.querySelector('input[name="credentials.passcode"]') !== null;
    const consentRequested = document.querySelector('input[type="submit"][value="Allow Access"]') !== null;
    return returnedToSwagger || passwordRequested || consentRequested;
  }, APP_ORIGIN, { timeout: 150_000 });

  if (await page.locator('input[name="credentials.passcode"]').isVisible()) {
    await page.fill('input[name="credentials.passcode"]', TEST_USER_PASSWORD);
    await page.click('[data-se="o-form-input-submit"], input[type="submit"]');
  }

  // ------------------------------------------------------------------
  // 8. Wait for redirect back to Swagger UI
  //    Spring Security redirects back to the same app origin after the OAuth2 callback.
  // ------------------------------------------------------------------
  const swaggerUrl = new RegExp(`${APP_ORIGIN.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}.*swagger-ui`);
  const allowAccess = page.getByRole('button', { name: 'Allow Access', exact: true });
  await expect.poll(async () =>
    swaggerUrl.test(page.url()) || await allowAccess.isVisible(),
  { timeout: 30_000 }).toBe(true);

  if (await allowAccess.isVisible()) {
    expect(new URL(page.url()).hostname).toBe(`${OKTA_TENANT_ID}.okta.com`);
    await expect(page.getByText('My Web App X', { exact: true })).toBeVisible();
    const scopes = page.locator('input[type="checkbox"][name^="optedScopes."]');
    expect(await scopes.evaluateAll(inputs => inputs.map(input => input.getAttribute('name')).sort()))
      .toEqual(['optedScopes.bar', 'optedScopes.email', 'optedScopes.foo']);
    await showInstructionBanner(page, 'TEST: Approving the expected application scopes automatically.');
    await allowAccess.click();
  }
  await page.waitForURL(swaggerUrl, { timeout: 30_000 });

  // ------------------------------------------------------------------
  // 9. Assertions
  // ------------------------------------------------------------------

  // Padlock must be locked (BffEnabler called authActions.authorize())
  await expect(page.locator('button.btn.authorize.locked').first()).toBeVisible({ timeout: 30_000 });

  // /shortprofile must confirm the session is active server-side
  // (this is the authoritative check — BffEnabler called it to establish the session)

  // /shortprofile should confirm the session is active server-side
  const profileResponse = await page.request.get(`${APP_ORIGIN}/shortprofile`);
  expect(profileResponse.status()).toBe(200);
  const profile = await profileResponse.json();
  expect(profile).toMatchObject({ loggedIn: true });
  expect(typeof profile.name).toBe('string');
  expect(profile.name.length).toBeGreaterThan(0);

  // ------------------------------------------------------------------
  // 10. Assert /secured/user returns 200 while logged in
  //     user@example.com is in the myuser Okta group → ROLE_myuser →
  //     Spring Security grants access → returns plain text "ok"
  // ------------------------------------------------------------------
  const securedResponse = await page.request.get(`${APP_ORIGIN}/secured/user`);
  expect(securedResponse.status()).toBe(200);
  expect(await securedResponse.text()).toBe('ok');

  // Inject a visible banner into the page for 4 seconds so you can see the
  // result before the test moves on to logout.
  //currently unable to see. will check later
  await page.evaluate(() => {
    const banner = document.createElement('div');
    banner.textContent = '✅ /secured/user → 200 OK  —  session is active. Proceeding to logout...';
    banner.style.cssText = [
      'position:fixed', 'top:0', 'left:0', 'width:100%',
      'padding:18px', 'background:#4caf50', 'color:#fff',
      'font-size:18px', 'font-weight:bold', 'z-index:99999', 'text-align:center',
    ].join(';');
    document.body.appendChild(banner);
  });
  await page.waitForTimeout(4_000);

  // ------------------------------------------------------------------
  // 11. Logout via /apilogout?source=swagger
  //     BFF clears the Spring Security session, then redirects to Okta's
  //     /v1/logout endpoint with id_token_hint. After Okta completes its
  //     side of the logout it redirects back to Swagger UI (post_logout_redirect_uri).
  // ------------------------------------------------------------------
  await page.goto(`${APP_ORIGIN}/apilogout?source=swagger`);
  await page.waitForURL(/swagger-ui/, { timeout: 30_000 });

  // ------------------------------------------------------------------
  // 12. Assert padlock is unlocked after logout
  //     BffEnabler re-checks /shortprofile on load; when it returns
  //     { loggedIn: false } the extension calls authActions.logout()
  //     which removes the locked CSS class from the Authorize button.
  // ------------------------------------------------------------------
  await expect(page.locator('button.btn.authorize:not(.locked)').first()).toBeVisible({ timeout: 15_000 });

  // ------------------------------------------------------------------
  // 13. Assert /shortprofile returns { loggedIn: false } after logout
  // ------------------------------------------------------------------
  const profileAfterLogout = await page.request.get(`${APP_ORIGIN}/shortprofile`);
  expect(profileAfterLogout.status()).toBe(200);
  const profileDataAfter = await profileAfterLogout.json();
  expect(profileDataAfter).toMatchObject({ loggedIn: false });

  // ------------------------------------------------------------------
  // 14. Assert /secured/user returns 401 after logout
  //     Session is gone — Spring Security's authenticationEntryPoint
  //     returns 401 Unauthorized for API calls (not a login redirect).
  // ------------------------------------------------------------------
  const securedAfterLogout = await page.request.get(`${APP_ORIGIN}/secured/user`);
  expect(securedAfterLogout.status()).toBe(401);

  // Keep the browser open for 5 seconds so you can see the final state
  await page.waitForTimeout(5_000);
});
