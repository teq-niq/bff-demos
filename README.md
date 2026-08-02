# bff-demos

This project is called BFF-demos because it demonstrates Backend-for-Frontend (BFF) security patterns, particularly with OIDC. It also explores BFF-style integrations in Swagger using Swagger UI’s plugin mechanism.  


![OpenAPI](https://img.shields.io/badge/OpenAPI-3.x-green?style=flat-square)
![Swagger UI](https://img.shields.io/badge/Swagger-UI-brightgreen?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=flat-square)
![Spring Security](https://img.shields.io/badge/Spring%20Security-enabled-brightgreen?style=flat-square)
![OIDC](https://img.shields.io/badge/Security-OIDC-blue?style=flat-square)
![Okta](https://img.shields.io/badge/Security-Okta-blue?style=flat-square)   
![Java](https://img.shields.io/badge/Java-24-blue?style=flat-square)
![Angular](https://img.shields.io/badge/Angular-red?style=flat-square)
![Node.js](https://img.shields.io/badge/Node.js-blue?style=flat-square)
![React](https://img.shields.io/badge/React-blue?style=flat-square)&nbsp;&nbsp;&nbsp;
![Maven](https://img.shields.io/badge/Build-Maven-red?style=flat-square)


The rationale for the BFF pattern, why it is preferred over PKCE for SPAs, and how it integrates with Swagger UI is covered in detail in the [earlier project](https://github.com/teq-niq/bff/). This project picks up from there.

For the BFF pattern to work, the apps must not be pure SPAs — the Angular front-end piggybacks on an HTTP session managed by the backend, keeping tokens server-side where they belong.

![BFF vs Standard SPA](images/bff-vs-pkce.svg)

What is new here is how the pieces are consumed. Instead of building the React-based Swagger UI BFF extension from source, this project consumes it as a Maven dependency via WebJars. Swagger UI with the BFF extension is served by the application itself at the app origin, with no separate origin.

The focus here is:
- Swagger UI served by the application itself through WebJars at the app origin
- Angular front-end development from source
- Two demo styles: a simple Spring Security app and an Okta OIDC app
- Setup closer to normal application development — consume the extension, don't build it

This README concentrates on how to run the apps, how to use Angular dev mode, and how to test the Swagger UI flows from the app origin.

## Projects

- `simple.bff` - Spring Security BFF demo with a local Angular front-end and WebJars-based Swagger UI
- `oidc.bff` - Okta OIDC BFF demo with a local Angular front-end and WebJars-based Swagger UI
- `okta-setup-for-oidc` - helper setup for the OIDC demo

## Prerequisites

- Java 24
- Maven 3.9.x
- Node.js is managed by the Maven build and the provided shell scripts
- A browser for the UI flows
- For the OIDC demo, a working Okta tenant and test user
- For OIDC setup steps, use `okta-setup-for-oidc` in this workspace

## First Build

Run this once from the root of `bff-demos`:

```bash
mvn clean package -P npmbuild
```

This is the one-time build. It downloads and assembles the support artifacts needed by the demos.

## What Is Different Here

In this workspace:

- Swagger UI is served by the application itself from WebJars at the app origin.  
- Angular is the only thing you are expected to edit in dev mode.  
- The backend and Swagger UI share the same browser origin in each demo.

## Our BFF App Examples

This diagram shows runtime flow.

![Runtime Layout](images/runtime-layout.svg)

## `simple.bff`

This is the Simple Spring Security demo. It runs on port `8080`.

### Start the backend

From the `bff-demos` root:

```bash
mvn -pl simple.bff spring-boot:run -P berun
```

### Swagger UI flow

Open Swagger UI from the app origin:

```text
http://localhost:8080/swagger-ui.html
```

The key screens for the simple demo are:

<img src="images/simple-swagger-bff-home-lock-highlighted.png" alt="Swagger UI home" width="400" />

Click the highlighted lock symbol Authorize button to open the login dialog.   

<img src="images/simple_swagger_login.png" alt="Swagger UI Login" width="400" />  

The credentials can be found here- simple.bff\src\main\java\com\example\demo\SecurityConfiguration.java - userDetailsService() method.  

<img src="images/simple_swagger_logged_on.png" alt="Swagger UI logged on" width="400" />

Post login press the close button to return to the Swagger UI home screen in a logged on state. The lock should be active and the session established in the backend.

<img src="images/simple-swagger-bff-home-loggedon.png" alt="Swagger UI home when logged on" width="400" />

The home screen shows the lock state. Clicking the lock opens the logged on dialog. 

From here please logout and the press close button to return to the Swagger UI home screen in a logged out state. The lock should be inactive and the session cleared in the backend.


### Angular flow   

```text
http://localhost:8080/
```
Note: you may need to first sign out in case you did not sign out from the Swagger UI flow.  The Angular app uses the same session as the Swagger UI flow. In case you didn't do that expect to see already logged on which is not an issue.  Just that the documented screens below will be different.  

<img src="images/angular_simple_landing.png" alt="Angular landing" width="400" />

Showing here the simple angular app when running as part of the application.  Click the Sign In button to go to the login screen.

<img src="images/simple_standalone_angular_login.png" alt="Angular login" width="400" />

Showing here the login screen served by the angular app.  
User credentials can be seen in simple.bff\src\main\java\com\example\demo\SecurityConfiguration.java.  

<img src="images/angular_standalone_postLogin.png" alt="Angular post login" width="400" />

Showing here a successful login. 

Do please explore the simple app.  

### Start Angular in dev mode

If you need to work on angular in dev mode you should indicate that to the back end.  
Stop the server and restart it.  

mvn -pl simple.bff spring-boot:run -P berun -Dfebaseurl=http://localhost:4200

The Angular sources are in `simple.bff/angular-front-end`.

Use the local shell wrapper first if you want the isolated Node/npm setup from the repo:

```bash
cd simple.bff
angularshell.sh 
```
or 

```cmd
cd simple.bff
angularshell.bat 
```

<img src="images/simple_angular_shell.png" alt="Angular dev shell" width="400" />

Then run:

```bash
npm start
```

If you are launching Angular manually without Maven using an IDE , make sure `serverenv.json` is present in `src/assets`. Maven copies it automatically when the backend is started with the `berun` profile.


### Angular flow in dev mode

The Angular app shows the login and post-login state like this:

<img src="images/simple_angular_login.png" alt="Angular login" width="400" />

<img src="images/simple_angular_post_login_all_buttons.png" alt="Angular post login" width="400" />



## `oidc.bff`

This is the Okta OIDC demo. It runs on port `8081`.

**Prerequisite**: complete the Okta app and user setup in [okta-setup-for-oidc](./okta-setup-for-oidc)  before running the OIDC demo.

### Start the backend

From the `bff-demos` root:

```bash
mvn -pl oidc.bff spring-boot:run -P berun -Dokta.tenant.id=[TENANT_ID] -Dokta.oauth2.client-id=[CLIENT_ID] -Dokta.oauth2.client-secret=[CLIENT_SECRET]
```
### Swagger UI flow

Open Swagger UI from the app origin:

```text
http://localhost:8081/swagger-ui.html
```

Showing below the first time login flow. Subsequent flows will be slightly different.  


<img src="images/oidc_swagger_ui_landing_lock_highlighted.png" alt="oidc swagger ui landing" width="400" />  

Click the highlighted lock symbol to open the login dialog.

<img src="images/oidc_swagger_ui_login1.png" alt="oidc swagger ui landing" width="400" />

The credentials will be as setup by you in the Okta setup.  
The username is the email address of the user you created in Okta.  
The password is the one you set for that user.   
See [okta-setup-for-oidc](./okta-setup-for-oidc)   


<img src="images/oidc_swagger_ui_login2.png" alt="oidc swagger ui landing" width="400" />

<img src="images/oidc_swagger_ui_login3.png" alt="oidc swagger ui landing" width="400" />

On clicking Stup you will be shown a QR code.  
You will have to scan it using the okta verify app.  

<img src="images/oidc_swagger_ui_login5.png" alt="oidc swagger ui landing" width="400" />

During your first login you will be shown your scope selection screen.  
You can select the scopes you want to grant to the application.  
Thereafter you will not be shown this screen again by default. 
The scopes you select will be stored in the Okta user profile and used for subsequent logins.
That said its also possible to force the scope selection screen to be shown again. Not discussed here.  

<img src="images/oidc_swagger_ui_loggedon.png" alt="oidc swagger ui landing" width="400" />

After the browser returns from Okta, the padlock should be active and the session should be established in the backend.

Click the padlock to see the logged on state. You can log out from here and the session will be cleared in the backend.

### Angular flow

```text
http://localhost:8081/
```

Assuming the first time login was already done in the Swagger UI flow, the Angular app will not go through the Okta first time login flow again.
The login flow will be slightly different from first time login and is shown below.   

<img src="images/oidc_angular_landing.png" alt="Angular landing" width="400" />

<img src="images/oidc_standalone_login.png" alt="Angular login" width="400" />

<img src="images/oidc_standalone_login1.png" alt="Angular login step 1" width="400" />

<img src="images/angular_standalone_login_select.png" alt="Angular login select" width="400" />


<img src="images/oidc_angular_standalone_login_code.png" alt="Angular login code" width="400" />


<img src="images/oidc_angular_standalone_login_password.png" alt="Angular login password" width="400" />


<img src="images/oidc_angular_logged_on.png" alt="Angular logged on" width="400" />



### Start Angular in dev mode

The Angular sources are in `oidc.bff/angular-front-end`.


If you need to work on angular in dev mode you should indicate that to the back end.
Stop the server and restart it.

mvn -pl oidc.bff spring-boot:run -P berun -Dokta.tenant.id=[TENANT_ID] -Dokta.oauth2.client-id=[CLIENT_ID] -Dokta.oauth2.client-secret=[CLIENT_SECRET] -Dfebaseurl=http://localhost:4201

Use the local shell wrapper first if you want the isolated Node/npm setup from the repo:

```bash
oidc.bff/angularshell.sh 
```
OR

```cmd
oidc.bff/angularshell.bat 
```

<img src="images/oidc_angular_shell.png" alt="Angular dev shell" width="400" />

Then run:

```bash
npm start
```
### Angular flow in dev mode

The OIDC Angular dev script uses port `4201` in the source copy.

<img src="images/oidc_angular_dev.png" alt="OIDC Angular app dev" width="400" />





## E2E Tests

The Playwright tests live in each module under `swagger-bff-e2e`.
Currently they test only the swagger ui login flow.  
We do have integration tests covering the angular , swagger ui back end API.

### Simple demo

```bash
mvn -pl simple.bff verify -Pe2e
```

This runs the simple form-login flow through Swagger UI and checks the authenticated and logged-out states.

### OIDC demo

```bash
mvn -pl oidc.bff verify -Pe2e -Dokta.tenant.id=[TENANT_ID] -Dokta.oauth2.client-id=[CLIENT_ID] -Dokta.oauth2.client-secret=[CLIENT_SECRET]
```

This runs the Okta redirect flow. The TOTP step is manual in the current test, so you type the 6-digit code in the browser when prompted.
Also note. Do not run this test before you have completed the Okta setup and done a first-time login in the Swagger UI flow or angular UI flow. 
The test will fail if you have not done that. Our test assumes first time login has been done.  
Might later enhance the oidc.bff E2E tests to also handle first time login in the future.  
## Notes

- The Angular shell wrappers are only there to give you a controlled Node/npm environment.
- The Maven `berun` profile is what copies `serverenv.json` and sets up the runtime paths for the front-end.
- Swagger UI is always opened from the application origin for each demo.
- If you rebuild the workspace, the generated `target` copies will refresh automatically.

## Troubleshooting

The [troubleshooting notes](notes/troubleshooting.md) capture issues encountered and their resolutions, and may be updated as needed.



