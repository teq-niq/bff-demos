# Automatic Okta Setup Guide

## Sign Up & Initial Setup

## Sign Up & Initial Setup

1. Visit https://developer.okta.com/signup/     
   Sign up for free 30 day trial account   
   OR   
   for free integrator account   
2. Activate your account.   
3. Set up your password.   
4. Install **Okta Verify** on your mobile device.   

lets say ur tenant id is trial-9999999 OR integrator-9999999   

Visit https://trial-9999999.okta.com OR   
https://integrator-9999999.okta.com   

Login.   
Will need to use your password and okta verify token,    
That should reach you to   
https://trial-999999-admin.okta.com/admin/getting-started   
OR to   
https://integrator-999999-admin.okta.com/admin/getting-started   

I have used the trial account more and only just started using the integrator account.   

## OKTA SDK API Token

Expand the admin console menu and navigate to Security → API → Tokens

Security → API → Tokens
https://trial-999999-admin.okta.com/admin/access/api/tokens
What do you want your token to be named? : myadmintoken
API calls made with this token must originate from : Any IP

## Setup


mvn -pl okta-setup-for-oidc exec:java -Dexec.mainClass=com.example.Setup -Dokta.tenant.id=[okta tenant id] -Dokta.api.token=[Okta API Token] -Ddummy.pwd=[Dummy password]

This will create two users alongwith other aspects of the setup:   
- user@example.com    
- admin@example.com    


## Teardown

mvn -pl okta-setup-for-oidc exec:java -Dexec.mainClass=com.example.Teardown -Dokta.tenant.id=[okta tenant id] -Dokta.api.token=[Okta API Token]



---

##  Overview Diagram 




Okta Tenant   
│   
├── Applications  
│ └── OIDC Web App (Client ID/Secret)   
│  
├── Directory   
│ ├── Users   
│ └── Groups (mygroup, myadmin, myuser)  
│  
├── Authorization Server (default)  
│ ├── Policies → Allow web App  
│ ├── Rules → Allow Authorization Code   
│ ├── Scopes → foo, bar, email (modified)   
│ └── Claims → groups (for ID + Access Token)  
