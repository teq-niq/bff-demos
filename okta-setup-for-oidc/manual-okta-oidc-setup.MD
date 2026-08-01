# Manual Okta Setup Guide

## Sign Up & Initial Setup

1. Visit https://developer.okta.com/signup/     
   Sign up for free 30 day trial account   
   OR   
   for free integrator account   
2. Activate your account.   
3. Set up your password.   
4. Install **Okta Verify** on your mobile device.   

lets say ur tenant id is trial-9999999 OR integrator-9999999  

9999999 is only a placeholder for the actual digits.   

Visit https://trial-9999999.okta.com OR   
https://integrator-9999999.okta.com   

Login.   
Will need to use your password and okta verify token,    
That should reach you to   
https://trial-999999-admin.okta.com/admin/getting-started   
OR to   
https://integrator-999999-admin.okta.com/admin/getting-started   

I have used the trial account more and only just started using the integrator account. 


### Groups


Expand the admin console menu and navigate to Directory > Groups
https://trial-999999-admin.okta.com/admin/groups


Create:
- `myuser`
- `myadmin`
- `mygroup`

Expand the admin console menu and navigate to Directory > People
https://trial-9999999-admin.okta.com/admin/users

### Users


Add multiple users.
Ensure each user belongs to mygroup.

Examples  
- user@example.com with group membership of mygroup, myuser.   
- admin@example.com with group membership of mygroup, myadmin.   

**Note**: Prefer using these emails. If you change to something else please change accordingly wherever else teh change is needed.   

### OIDC Application
Expand the admin console menu and navigate to Applicatioms > Applicatioms   

https://trial-9999999-admin.okta.com/admin/apps/active
Click Create App Integration> Create a new app integration
OIDC - OpenID Connect 
Application Type: Web Application  
### Configuration

**General**
- App Integration Name: `Any name`
- Grant Types: Authorization Code, Refresh Token
- User Consent: Require Consent

**Sign-in redirect URIs:**
http://localhost:8081/login/oauth2/code/okta  


**Sign-out redirect URIs:**
http://localhost:8081
http://localhost:8081/swagger-ui/index.html
http://localhost:3201
http://localhost:4201


Ensure: **Federation Broker Mode:** Disabled

**Important:** Note down **Client ID** and **Client Secret**.


## Authorization Server Configuration

Expand the admin console menu and navigate to Security > API > Authorization Servers > default

https://trial-9999999-admin.okta.com/admin/oauth2/as

Click default.

### Scopes

Navigation:

Security → API → Authorization Servers → default → Scopes


Add:
- `foo` (User consent: Optional)
- `bar` (User consent: Optional)

Modify existing:
- `email` → Set **User consent: Required**

##  Claims


Security → API → Authorization Servers → default → Claims


### Claim: groups (Access Token)

- Name: `groups`
- Include in: **Access Token → Always**
- Value type: Groups
- Filter:
  - Starts with: `my`
- Include in: Any scope
- Disable: false

Save.

### Claim: groups (ID Token)

Repeat above, but:
- Include in: **ID Token → Always**
---

###  Access Policy


Security → API → Authorization Servers → default → Access Policies

Go to **Access Policies → Add Policy**.

- Name: `Allow web App`
- Description: `Allow web App`
- Assign To: All clients

Create policy → Add rule.


#### Add Policy Rule

**Rule Name:** `Allow Authorization Code`

Conditions:
- Grant type: Authorization Code
- User: Any user assigned the app
- Scopes: Any scopes
- Inline Hook: None
- Access token lifetime: **1 hour**
- Refresh token lifetime: **90 days**
  - Expires if unused for **7 days**


---



Security → API → Tokens
https://trial-4304447-admin.okta.com/admin/access/api/tokens
What do you want your token to be named? : myadmintoken
API calls made with this token must originate from : Any IP


---

## Overview Diagram 




Okta Tenant   
│   
├── Applications  
│ └── OIDC Web App (Client ID/Secret)   
│  
├── Directory   
│ ├── Users   
│ └── Groups (mygroup, myadmin, etc.)  
│  
├── Authorization Server (default)  
│ ├── Policies → Allow web App  
│ ├── Rules → Allow Authorization Code   
│ ├── Scopes → foo, bar, email (modified)   
│ └── Claims → groups (for ID + Access Token)  
