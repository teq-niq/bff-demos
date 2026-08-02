# Integration Tests for spring.oidc.bff

This directory contains comprehensive integration tests for the OIDC Backend-For-Frontend (BFF) module.

## Quick Start

### Run All Tests
```bash
# From project root
mvn -pl bff-spring-projs/spring.oidc.bff test

# Or from module directory
cd bff-spring-projs/spring.oidc.bff
mvn test
```

### Run Specific Test Class
```bash
mvn -pl bff-spring-projs/spring.oidc.bff test -Dtest=SecuredUserEndpointTest
mvn -pl bff-spring-projs/spring.oidc.bff test -Dtest=SecurityConfigurationIntegrationTest
```

## No Real Okta Credentials Required

These tests **DO NOT** require real Okta credentials or network connectivity. The test configuration:
- Uses stub OAuth2 properties that bypass Okta's environment post-processor
- Mocks OIDC authentication using Spring Security Test's `oidcLogin()` post-processor
- Never makes network calls to Okta servers

## Test Structure

### Test Files
```
src/test/java/com/example/demo/
├── CheckPostEndpointTest.java                    # POST endpoint with CSRF tests
├── SecuredAdminEndpointTest.java                 # ROLE_myadmin authorization tests
├── SecuredBarEndpointTest.java                   # SCOPE_bar authorization tests
├── SecuredFooEndpointTest.java                   # SCOPE_foo authorization tests
├── SecuredProfileEndpointTest.java               # Authenticated-only endpoint tests
├── SecuredUserEndpointTest.java                  # ROLE_myuser authorization tests
├── SecurityConfigurationIntegrationTest.java     # Comprehensive security matrix tests
└── ShortProfileEndpointTest.java                 # Profile endpoint with JSON assertions
```

### Test Configuration
```
src/test/resources/
└── application.properties                        # Stub OAuth2 config (no real credentials)
```

## What's Being Tested

### 1. Role-Based Authorization
Tests that Okta groups mapped to Spring Security roles work correctly:
- `ROLE_myuser` (from Okta group "myuser")
- `ROLE_myadmin` (from Okta group "myadmin")

### 2. Scope-Based Authorization
Tests that OAuth2 scopes are enforced:
- `SCOPE_foo` (from OAuth2 scope "foo")
- `SCOPE_bar` (from OAuth2 scope "bar")

### 3. Authentication Requirements
Tests that endpoints requiring authentication return 401 when unauthenticated.

### 4. Public Endpoints
Tests that public endpoints ( `/shortprofile`, `/checkpost`) are accessible without authentication.

### 5. CSRF Protection
Tests that POST endpoints require CSRF tokens.

## Test Coverage

| Endpoint | Authorization Rule | Test Class | Tests |
|----------|-------------------|------------|-------|
| `/secured/user` | hasRole("myuser") | SecuredUserEndpointTest | 5 |
| `/secured/admin` | hasRole("myadmin") | SecuredAdminEndpointTest | 5 |
| `/secured/foo` | hasAuthority("SCOPE_foo") | SecuredFooEndpointTest | 6 |
| `/secured/bar` | hasAuthority("SCOPE_bar") | SecuredBarEndpointTest | 6 |
| `/secured/profile` | authenticated() | SecuredProfileEndpointTest | 5 |
| `/shortprofile` | permitAll() | ShortProfileEndpointTest | 6 |
| `/checkpost` | permitAll() | CheckPostEndpointTest | 4 |
| All endpoints | Security matrix | SecurityConfigurationIntegrationTest | 10 |

**Total: 53 test methods across 9 test classes**

## Understanding the Tests

### Example: Role-Based Test
```java
@Test
void testSecuredUser_AsMyuser_Returns200() throws Exception {
    mockMvc.perform(get("/secured/user")
            .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_myuser"))))
        .andExpect(status().isOk())
        .andExpect(content().string("ok"));
}
```

This test:
1. Mocks an OIDC user with `ROLE_myuser` authority
2. Makes a GET request to `/secured/user`
3. Expects HTTP 200 (OK) response
4. Verifies response body is "ok"

### Example: Scope-Based Test
```java
@Test
void testSecuredFoo_WithScopeFoo_Returns200() throws Exception {
    mockMvc.perform(get("/secured/foo")
            .with(oidcLogin().authorities(new SimpleGrantedAuthority("SCOPE_foo"))))
        .andExpect(status().isOk())
        .andExpect(content().string("ok"));
}
```

This test:
1. Mocks an OIDC user with `SCOPE_foo` authority
2. Makes a GET request to `/secured/foo`
3. Expects HTTP 200 (OK) response
4. Verifies response body is "ok"

### Example: JSON Response Test
```java
@Test
void testShortProfile_AsMyuser_ReturnsProfileWithRole() throws Exception {
    mockMvc.perform(get("/shortprofile")
            .with(oidcLogin()
                .attributes(attrs -> {
                    attrs.put("name", "Test User");
                    attrs.put("email", "test@example.com");
                })
                .authorities(new SimpleGrantedAuthority("ROLE_myuser"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loggedIn").value(true))
        .andExpect(jsonPath("$.name").exists())
        .andExpect(jsonPath("$.roles", hasItem("ROLE_myuser")));
}
```

This test:
1. Mocks an OIDC user with attributes and role
2. Makes a GET request to `/shortprofile`
3. Expects HTTP 200 (OK) response
4. Validates JSON response structure and values

## Security Configuration Being Tested

The tests verify the security configuration in `SecurityConfiguration.java`:

```java
// Role mapping from Okta groups
customOidcUserService() maps:
  - Okta group "myuser" → ROLE_myuser
  - Okta group "myadmin" → ROLE_myadmin

// Authorization rules
authorizeHttpRequests(auth -> auth
    .requestMatchers("/secured/profile").authenticated()
    .requestMatchers("/secured/admin").hasRole("myadmin")
    .requestMatchers("/secured/user").hasRole("myuser")
    .requestMatchers("/secured/foo").hasAuthority("SCOPE_foo")
    .requestMatchers("/secured/bar").hasAuthority("SCOPE_bar")
    .anyRequest().permitAll()
)
```

## Running Tests in IDE

### IntelliJ IDEA
1. Right-click on test class or test method
2. Select "Run 'TestClassName'" or "Run 'testMethodName()'"

### Eclipse
1. Right-click on test class or test method
2. Select "Run As" → "JUnit Test"

### VS Code
1. Install Java Test Runner extension
2. Click "Run Test" code lens above test method

## Troubleshooting

### Tests fail with network errors
If you see `Connection refused` or similar errors, it means the test configuration is trying to contact Okta. This should NOT happen with the provided `application.properties`. Verify:
- You're using `src/test/resources/application.properties` (not main resources)
- The properties file uses `spring.security.oauth2.*` NOT `okta.*`

### Tests fail with "No qualifying bean"
This usually means the Spring context didn't start properly. Check:
- All dependencies in `pom.xml` are available
- Java version matches project requirements
- Run `mvn clean install` from parent directory first

### CSRF tests fail
Make sure you're using `.with(csrf())` in POST request tests:
```java
mockMvc.perform(post("/checkpost")
    .contentType(MediaType.APPLICATION_JSON)
    .content("{}")
    .with(csrf()))  // Required for POST requests
```

## CI/CD Integration

These tests are designed to run in CI/CD pipelines without any external dependencies:

```yaml
# Example GitHub Actions
- name: Run Integration Tests
  run: mvn -pl bff-spring-projs/spring.oidc.bff test
```

```yaml
# Example GitLab CI
test:
  script:
    - mvn -pl bff-spring-projs/spring.oidc.bff test
```

## Additional Resources

- [Spring Security Test Documentation](https://docs.spring.io/spring-security/reference/servlet/test/index.html)
- [MockMvc Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#spring-mvc-test-framework)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)

## Test Summary

For a detailed analysis of all generated tests, see [TEST_GENERATION_SUMMARY.md](TEST_GENERATION_SUMMARY.md).
