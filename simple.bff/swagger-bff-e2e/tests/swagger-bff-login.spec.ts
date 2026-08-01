/**
 * E2E test: Swagger UI BFF login via inline form (spring.simple.bff, port 8080)
 *
 * Flow:
 *   1. Open Swagger UI  →  click Authorize
 *   2. BFF extension renders the inline login form (redirectforlogin: false)
 *   3. Fill username and password  →  click Authorize in the dialog
 *   4. BFF plugin POSTs credentials to /login, then GETs /shortprofile
 *   5. SwaggerUI padlock locks (BffPlugin calls oriAuthorize with profile data)
 *   6. Assert padlock is locked + /shortprofile returns { loggedIn: true }
 *   7. Assert /secured/user returns 200 (user has ROLE_myuser)
 *   8. Click the locked padlock  →  click Logout in the dialog
 *   9. BFF plugin GETs /logout, then calls oriLogout to clear Swagger UI state
 *  10. Close the dialog  →  assert padlock is unlocked
 *  11. Assert /shortprofile returns { loggedIn: false }
 *  12. Assert /secured/user returns 401 (session gone)
 *
 * Prerequisites:
 *   - BFF app running:  cd bff && mvn -pl bff-spring-projs/spring.simple.bff spring-boot:run -P berun
 *   - Test credentials defined in SecurityConfiguration.java:
 *       user  / password  → ROLE_myuser
 *       admin / password  → ROLE_myadmin
 *
 * Run via Maven (Swagger UI served from the BFF, port 8080):
 *   mvn -pl bff-spring-projs/spring.simple.bff verify -Pe2e
 *
 * Run directly (app already running, env vars set manually):
 *   cd bff-spring-projs/spring.simple.bff/swagger-bff-e2e
 *   $env:SWAGGER_UI_URL="http://localhost:8080"
 *   npx playwright test --headed
 */

import { test, expect } from '@playwright/test';

// ---------------------------------------------------------------------------
// Credentials / config
// SWAGGER_UI_URL — the single app origin used for Swagger UI and BFF calls.
// Injected by the Maven e2e profile.
//
// TEST_USER_USERNAME / TEST_USER_PASSWORD — defaults match SecurityConfiguration.java.
// ---------------------------------------------------------------------------
const APP_ORIGIN = process.env.SWAGGER_UI_URL ?? 'http://localhost:8080';
const APP_URL = `${APP_ORIGIN}/swagger-ui/index.html`;
const TEST_USER_USERNAME = process.env.TEST_USER_USERNAME ?? 'user';
const TEST_USER_PASSWORD = process.env.TEST_USER_PASSWORD ?? 'password';

// ---------------------------------------------------------------------------
// Test
// ---------------------------------------------------------------------------
test('Swagger UI BFF login — inline form flow (simple Spring Security)', async ({ page }) => {

  // ------------------------------------------------------------------
  // 0. Ensure a clean session before starting.
  //    A leftover session cookie from a previous run would cause BffEnabler's
  //    /shortprofile fetch to return { loggedIn: true }, triggering authorize()
  //    mid-test and switching the dialog from the login form to the "Authorized"
  //    view while typing is in progress.
  // ------------------------------------------------------------------
  await page.request.get(`${APP_ORIGIN}/apilogout?source=swagger`);

  // ------------------------------------------------------------------
  // 1. Open Swagger UI
  // ------------------------------------------------------------------
  await page.goto(APP_URL);
  // Wait for SwaggerUI bundle to finish rendering the spec title block
  await page.waitForSelector('.swagger-ui .info', { timeout: 15_000 });
  // Wait for BffEnabler's /shortprofile fetch to complete so the Redux auth
  // state is settled before we open the Authorize dialog.
  await page.waitForLoadState('networkidle');

  // ------------------------------------------------------------------
  // 2. Click the top-level "Authorize" button
  //    BffEnabler has already fetched /shortprofile on mount;
  //    since the session is fresh the padlock is unlocked.
  // ------------------------------------------------------------------
  await page.click('button.btn.authorize');

  // ------------------------------------------------------------------
  // 3. Wait for the BFF inline login form
  //    http-auth.jsx renders the form when redirectforlogin === false.
  //    Selectors from: swagger-ui/src/core/plugins/oas3/components/auth/http-auth.jsx
  // ------------------------------------------------------------------
  await page.waitForSelector('#auth-bff-username', { state: 'visible', timeout: 15_000 });
  // Use the native HTMLInputElement setter inside evaluate() — the most reliable
  // approach for React inputs in Playwright. pressSequentially/fill don't
  // consistently trigger React's onChange in the bundled Swagger UI (React 18,
  // stale-state batching). The native setter sets the DOM value directly and the
  // bubbling input/change events are picked up by React's event delegation.
  // A 200 ms gap between username and password lets React commit the setState
  // so HttpAuth.onChange reads the updated this.state.value for the second field.
  await page.locator('#auth-bff-username').evaluate((el, val) => {
    const input = el as HTMLInputElement;
    input.focus();
    Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value')!
      .set!.call(input, val);
    input.dispatchEvent(new Event('input',  { bubbles: true }));
    input.dispatchEvent(new Event('change', { bubbles: true }));
  }, TEST_USER_USERNAME);
  await page.waitForTimeout(200);
  await page.locator('#auth-bff-password').evaluate((el, val) => {
    const input = el as HTMLInputElement;
    input.focus();
    Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value')!
      .set!.call(input, val);
    input.dispatchEvent(new Event('input',  { bubbles: true }));
    input.dispatchEvent(new Event('change', { bubbles: true }));
  }, TEST_USER_PASSWORD);
  await page.waitForTimeout(200);


  // ------------------------------------------------------------------
  // 4. Click "Authorize" inside the dialog
  //    Selector from: swagger-ui/src/core/components/auth/auths.jsx
  //    <Button type="submit" className="btn modal-btn auth authorize" ...>Authorize</Button>
  //    The BFF plugin's wrapActions.authorize intercepts this, POSTs to /login,
  //    then GETs /shortprofile and calls oriAuthorize with the profile data.
  // ------------------------------------------------------------------
  await page.click('button.btn.modal-btn.auth.authorize');

  // ------------------------------------------------------------------
  // 5. Wait for the padlock to lock
  //    BffPlugin calls oriAuthorize → Swagger UI adds the .locked CSS class.
  // ------------------------------------------------------------------
  await expect(page.locator('button.btn.authorize.locked').first()).toBeVisible({ timeout: 15_000 });

  // Close the auth dialog — after oriAuthorize the dialog is still open
  // showing the "Authorized" view.  The modal overlay would otherwise block
  // the padlock click in step 8.
  await page.click('button.close-modal');

  // ------------------------------------------------------------------
  // 6. Assert /shortprofile confirms an active server-side session
  // ------------------------------------------------------------------
  const profileResponse = await page.request.get(`${APP_ORIGIN}/shortprofile`);
  expect(profileResponse.status()).toBe(200);
  const profile = await profileResponse.json();
  expect(profile).toMatchObject({ loggedIn: true });
  expect(typeof profile.name).toBe('string');
  expect(profile.name.length).toBeGreaterThan(0);

  // ------------------------------------------------------------------
  // 7. Assert /secured/user returns 200 while logged in
  //    'user' has ROLE_myuser → Spring Security grants access → "ok"
  // ------------------------------------------------------------------
  const securedResponse = await page.request.get(`${APP_ORIGIN}/secured/user`);
  expect(securedResponse.status()).toBe(200);
  expect(await securedResponse.text()).toBe('ok');

  // Inject a visible banner for 4 seconds before moving on to logout
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
  // 8. Re-open the auth dialog via the locked padlock
  //    When already authorized, Swagger UI shows the "Authorized" view
  //    with a Logout button.
  //    Selector from auths.jsx:
  //    <Button className="btn modal-btn auth" aria-label="Remove authorization">Logout</Button>
  // ------------------------------------------------------------------
  await page.click('button.btn.authorize.locked');
  await page.waitForSelector('[aria-label="Remove authorization"]', { timeout: 15_000 });
  await page.click('[aria-label="Remove authorization"]');

  // ------------------------------------------------------------------
  // 9. The BFF plugin's wrapActions.logout fires:
  //    - GETs /apilogout (custom endpoint that manually clears the SecurityContext)
  //    - Then calls oriLogout to clear Swagger UI's auth state
  //    After oriLogout, the dialog still shows the login form; close it.
  // ------------------------------------------------------------------
  await page.waitForSelector('button.close-modal', { timeout: 15_000 });
  await page.click('button.close-modal');

  // ------------------------------------------------------------------
  // 10. Assert padlock is unlocked after logout
  //     BffPlugin called oriLogout → .locked CSS class is removed.
  // ------------------------------------------------------------------
  await expect(page.locator('button.btn.authorize:not(.locked)').first()).toBeVisible({ timeout: 15_000 });

  // ------------------------------------------------------------------
  // 11. Assert /shortprofile returns { loggedIn: false } after logout
  // ------------------------------------------------------------------
  const profileAfterLogout = await page.request.get(`${APP_ORIGIN}/shortprofile`);
  expect(profileAfterLogout.status()).toBe(200);
  const profileDataAfter = await profileAfterLogout.json();
  expect(profileDataAfter).toMatchObject({ loggedIn: false });

  // ------------------------------------------------------------------
  // 12. Assert /secured/user returns 401 after logout
  //     Session is gone — Spring Security's authenticationEntryPoint
  //     returns 401 Unauthorized for API calls.
  // ------------------------------------------------------------------
  const securedAfterLogout = await page.request.get(`${APP_ORIGIN}/secured/user`);
  expect(securedAfterLogout.status()).toBe(401);

  // Show logged-out banner for 4 seconds so the result is visible
  await page.evaluate(() => {
    const banner = document.createElement('div');
    banner.textContent = '🔓 Logged out — /shortprofile: loggedIn=false  |  /secured/user: 401  ✅';
    banner.style.cssText = [
      'position:fixed', 'top:0', 'left:0', 'width:100%',
      'padding:18px', 'background:#1565c0', 'color:#fff',
      'font-size:18px', 'font-weight:bold', 'z-index:99999', 'text-align:center',
    ].join(';');
    document.body.appendChild(banner);
  });
  await page.waitForTimeout(4_000);
});
