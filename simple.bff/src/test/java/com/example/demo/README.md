# Spring Simple BFF - Integration Test Documentation

## Overview

This directory contains comprehensive integration tests for the `spring.simple.bff` module. The tests validate all endpoints, security configurations, role-based access control, and response assertions.

## Test Files Generated


### 2. **CheckPostEndpointTest.java** (6 tests)
Tests the `/checkpost` POST endpoint which accepts JSON payloads and returns a success message.

**Coverage:**
- ✅ Unauthenticated without CSRF token (403 Forbidden)
- ✅ Unauthenticated with CSRF token (200 OK)
- ✅ Authenticated as "user" (200 OK)
- ✅ Authenticated as "admin" (200 OK)
- ✅ Empty JSON payload (200 OK)
- ✅ Complex nested JSON payload (200 OK)

**Key Features:**
- CSRF protection validation
- JSON request body handling
- JSONPath assertions: `$.message` = "POST request received successfully"

**Key Endpoint Behavior:** `permitAll()` - Public access with CSRF protection

---

### 3. **ApiLogoutEndpointTest.java** (5 tests)
Tests the `/apilogout` endpoint which clears the security context and invalidates the session.

**Coverage:**
- ✅ Unauthenticated access (200 OK)
- ✅ Authenticated as "user" (200 OK)
- ✅ Authenticated as "admin" (200 OK)
- ✅ With source parameter "frontend" (200 OK)
- ✅ With source parameter "swagger" (200 OK)

**Key Endpoint Behavior:** `permitAll()` - Public access, clears session

---

### 4. **ShortProfileEndpointTest.java** (5 tests)
Tests the `/shortprofile` endpoint which returns user profile information with roles.

**Coverage:**
- ✅ Unauthenticated access (loggedIn: false)
- ✅ Authenticated as "user" (returns name, ROLE_myuser)
- ✅ Authenticated as "admin" (returns name, ROLE_myadmin)
- ✅ User with multiple roles (returns all roles)
- ✅ Custom user with custom role (returns custom role)

**JSON Response Assertions:**
```json
{
  "loggedIn": true/false,
  "name": "username",
  "roles": ["ROLE_myuser", "ROLE_myadmin"]
}
```

**Key Endpoint Behavior:** `permitAll()` - Public access, response varies by authentication

---

### 5. **SecuredUserEndpointTest.java** (6 tests)
Tests the `/secured/user` endpoint which requires the `myuser` role.

**Coverage:**
- ✅ Unauthenticated access (401 Unauthorized)
- ✅ Authenticated as "user" with ROLE_myuser (200 OK, returns "ok")
- ✅ Authenticated as "admin" with ROLE_myadmin (403 Forbidden)
- ✅ Authenticated with wrong role (403 Forbidden)
- ✅ User with both roles (200 OK)
- ✅ Authenticated without any roles (403 Forbidden)

**Security Rule:** `.requestMatchers("/secured/user").hasRole("myuser")`

**Key Endpoint Behavior:** Requires `ROLE_myuser`

---

### 6. **SecuredAdminEndpointTest.java** (6 tests)
Tests the `/secured/admin` endpoint which requires the `myadmin` role.

**Coverage:**
- ✅ Unauthenticated access (401 Unauthorized)
- ✅ Authenticated as "user" with ROLE_myuser (403 Forbidden)
- ✅ Authenticated as "admin" with ROLE_myadmin (200 OK, returns "ok")
- ✅ Authenticated with wrong role (403 Forbidden)
- ✅ User with both roles (200 OK)
- ✅ Authenticated without any roles (403 Forbidden)

**Security Rule:** `.requestMatchers("/secured/admin").hasRole("myadmin")`

**Key Endpoint Behavior:** Requires `ROLE_myadmin`

---

### 7. **SecuredProfileEndpointTest.java** (5 tests)
Tests the `/secured/profile` endpoint which requires any authenticated user (endpoint not implemented, tests security).

**Coverage:**
- ✅ Unauthenticated access (401 Unauthorized)
- ✅ Authenticated as "user" (passes security, 404 Not Found)
- ✅ Authenticated as "admin" (passes security, 404 Not Found)
- ✅ Authenticated with any role (passes security)
- ✅ Authenticated without roles (passes security)

**Security Rule:** `.requestMatchers("/secured/profile").authenticated()`

**Key Endpoint Behavior:** Requires authentication (any authenticated user)

---

### 8. **SimpleBffIntegrationTestSuite.java**
Test suite aggregator that provides comprehensive documentation and runs all tests.

---

## Test Statistics

| Metric | Value |
|--------|-------|
| **Total Test Classes** | 7 |
| **Total Test Methods** | 37 |
| **Endpoints Tested** | 6 (+ 1 security-only) |
| **Security Scenarios** | Unauthenticated, User Role, Admin Role, Wrong Role, Multiple Roles |
| **Response Assertions** | Status Codes, JSON Paths, String Content |

---

## Security Configuration Summary

### Users (from `SecurityConfiguration.java`)
```java
User "user":
  - Username: "user"
  - Password: "password" (BCrypt encoded)
  - Role: "myuser" → Spring Security: ROLE_myuser

User "admin":
  - Username: "admin"
  - Password: "password" (BCrypt encoded)
  - Role: "myadmin" → Spring Security: ROLE_myadmin
```

### Authorization Rules
```java
.requestMatchers("/secured/profile").authenticated()       // Any authenticated user
.requestMatchers("/secured/admin").hasRole("myadmin")      // Requires ROLE_myadmin
.requestMatchers("/secured/user").hasRole("myuser")        // Requires ROLE_myuser
.anyRequest().permitAll()                                   // All other endpoints public
```

### CSRF Protection
- **Enabled** for all endpoints
- POST requests require `.with(csrf())` in tests
- Uses `CookieCsrfTokenRepository.withHttpOnlyFalse()`
- Custom `SpaCsrfTokenRequestHandler` for SPA support

---

## Running the Tests

### Prerequisites
```bash
cd bff-spring-projs/spring.simple.bff
mvn clean compile
```

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=SecuredUserEndpointTest
mvn test -Dtest=CheckPostEndpointTest
mvn test -Dtest=ShortProfileEndpointTest
```

### Run Specific Test Method
```bash
mvn test -Dtest=SecuredUserEndpointTest#testSecuredUser_AsUser_Returns200
mvn test -Dtest=CheckPostEndpointTest#testCheckPost_AsUser_Returns200
```

### Run Test Suite
```bash
mvn test -Dtest=SimpleBffIntegrationTestSuite
```

### Generate Test Reports
```bash
mvn surefire-report:report
# Report location: target/surefire-reports/
# HTML report: target/site/surefire-report.html
```

### Run with Verbose Output
```bash
mvn test -X
```

### Run Tests in Parallel (if configured)
```bash
mvn test -Dparallel=classes -DthreadCount=4
```

---

## Test Patterns and Annotations

### Spring Boot Test Annotations
```java
@SpringBootTest              // Loads full Spring application context
@AutoConfigureMockMvc       // Auto-configures MockMvc for HTTP testing
```

### Security Test Annotations
```java
@WithMockUser(username = "user", roles = {"myuser"})    // Mock authenticated user
@WithMockUser(username = "admin", roles = {"myadmin"})  // Mock authenticated admin
```

### MockMvc Request Examples
```java
// GET request
mockMvc.perform(get("/hello"))
    .andExpect(status().isOk())
    .andExpect(content().string("user"));

// POST request with CSRF and JSON
mockMvc.perform(post("/checkpost")
    .with(csrf())
    .contentType(MediaType.APPLICATION_JSON)
    .content("{\"abc\":\"value\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.message").value("POST request received successfully"));
```

### Response Assertions
```java
// Status code assertions
.andExpect(status().isOk())                  // 200
.andExpect(status().isUnauthorized())        // 401
.andExpect(status().isForbidden())           // 403
.andExpect(status().isNotFound())            // 404

// Content assertions
.andExpect(content().string("ok"))           // Plain text
.andExpect(jsonPath("$.message").value("...")) // JSON field
.andExpect(jsonPath("$.roles", hasItem("ROLE_myuser"))) // JSON array contains
```

---

## Expected Test Results

### Success Criteria
All 37 tests should pass with:
- ✅ Correct status codes (200, 401, 403)
- ✅ Expected response bodies (JSON and plain text)
- ✅ Proper role-based access control
- ✅ CSRF protection validation

### Sample Output
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.example.demo.HelloEndpointTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.example.demo.CheckPostEndpointTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.example.demo.ApiLogoutEndpointTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.example.demo.ShortProfileEndpointTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.example.demo.SecuredUserEndpointTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.example.demo.SecuredAdminEndpointTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.example.demo.SecuredProfileEndpointTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 37, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] BUILD SUCCESS
```

---

## Test Coverage Matrix

| Endpoint | Unauthenticated | User Role | Admin Role | Wrong Role | Multiple Roles | Public Access |
|----------|----------------|-----------|------------|------------|----------------|---------------|
| `/checkpost` | ✅ 200* | ✅ 200 | ✅ 200 | N/A | N/A | Yes (permitAll) |
| `/apilogout` | ✅ 200 | ✅ 200 | ✅ 200 | N/A | N/A | Yes (permitAll) |
| `/shortprofile` | ✅ 200 | ✅ 200 | ✅ 200 | ✅ 200 | ✅ 200 | Yes (permitAll) |
| `/secured/user` | ❌ 401 | ✅ 200 | ❌ 403 | ❌ 403 | ✅ 200 | No (role required) |
| `/secured/admin` | ❌ 401 | ❌ 403 | ✅ 200 | ❌ 403 | ✅ 200 | No (role required) |
| `/secured/profile` | ❌ 401 | ✅ 404** | ✅ 404** | ✅ 404** | ✅ 404** | No (auth required) |

*Requires CSRF token  
**Passes security but endpoint not implemented (404)

---

## Troubleshooting

### Test Failures

**401 Unauthorized on public endpoints**
- Check if security configuration has changed
- Verify `permitAll()` rule is still in place

**403 Forbidden instead of expected 401**
- User is authenticated but lacks required role
- Check `@WithMockUser` roles match security requirements

**CSRF-related failures**
- Ensure POST requests use `.with(csrf())`
- Verify CSRF protection is enabled in SecurityConfiguration

**JSON assertion failures**
- Check response structure with `.andDo(print())`
- Verify jsonPath expression syntax

### Debug Mode
```java
// Add to test method for detailed output
mockMvc.perform(get("/hello"))
    .andDo(print())  // Prints full request/response
    .andExpect(status().isOk());
```

---

## Dependencies Required

Ensure `pom.xml` includes:
```xml
<!-- Spring Boot Test Starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Spring Security Test -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- JUnit 5 Platform Suite (for test suite) -->
<dependency>
    <groupId>org.junit.platform</groupId>
    <artifactId>junit-platform-suite-api</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Maintenance

### Adding New Endpoint Tests
1. Create new test class: `[EndpointName]EndpointTest.java`
2. Follow existing patterns with `@SpringBootTest` and `@AutoConfigureMockMvc`
3. Test all security scenarios: unauthenticated, correct role, wrong role
4. Add response body assertions (status codes, content, JSON)
5. Update this README with new test coverage

### Updating Existing Tests
When security rules or endpoints change:
1. Update corresponding test class
2. Verify role requirements match SecurityConfiguration
3. Update assertions if response format changes
4. Run full test suite to ensure no regressions
5. Update test coverage matrix in this README

---

## Related Files

- **Source Code:** `src/main/java/com/example/demo/SimpleBffApplication.java`
- **Security Config:** `src/main/java/com/example/demo/SecurityConfiguration.java`
- **Test Reports:** `target/surefire-reports/`
- **Test Classes:** `src/test/java/com/example/demo/`

---

## Summary

This comprehensive test suite provides:
- ✅ **100% endpoint coverage** for all implemented endpoints
- ✅ **Security validation** for all authorization rules
- ✅ **Role-based access control** testing with multiple scenarios
- ✅ **Response assertions** for both status codes and content
- ✅ **CSRF protection** validation for POST requests
- ✅ **JSON and plain text** response validation
- ✅ **37 test cases** covering success and failure scenarios

The tests are designed to catch security misconfigurations, authorization bugs, and response format issues early in the development cycle.
