---
date: 2016-10-12T19:07:43-04:00
title: Security
---

API security is very important and today APIs doesn't have security built-in at all but
relying on third party API gateway to handle the security at network level. This assumes
that within the organization, it is safe or some sort of firewall setup to ensure that
only Gateway host can access the service host. 

Once we move to the cloud, everything is dynamic and firewall won't work anymore as services
can be down on one VM but started in another VM immediately with container orchestration tools.

The traditional gateway also add another layer of network calls and if there are so many 
service to service calls, the added up latency is unacceptable. 

Here is an [article](https://networknt.github.io/light-4j/architecture/gateway/) 
in the architecture section that talks about the drawbacks of API gateway and why it is not 
suitable for microservices architecture. 

In order to make sure that security works for microservies, we have extended the OAuth 2.0
specification and come up with a branch new approach for authorization with JWT. Here is an
[article](https://networknt.github.io/light-4j/architecture/security/) talks about the API
security in light-*-4j frameworks. 

Basically, we have [light-oauth2](https://github.com/networknt/light-oauth2) OAuth 2.0 provider 
and built-in JWT security verification in the frameworks. The security policy is managed by the 
OAuth2 provider with service registration and client registration; however, the policy enforcement 
is done distributedly at each service level. With PIK signed JWT token issued from the light-oauth, 
all services can verify the token with the JWT signature public key certificate. 

The following is a list of key components in light-4j security.

## light-oauth2 server

Light-4j and related frameworks support OAuth 2.0 and related specifications to authorize service 
access. By default, the framework contains two pairs of public key certificates issued by our own 
testing oauth2 server which can be installed from Docker. For more info, please refer to 
https://github.com/networknt/light-oauth2

Light-4j also provides a client module that can communicate with light-oauth2 in the background to
get access token and renew the access token before it is expired. 

## Kid

Since services are deployed in the cloud without static IP, the traditional push certificates to each 
service is not working anymore. In this framework each service will pull certificate from OAuth2 server 
key service by a kid from JWT token header if the key doesn't exist locally. This approach that uses
kid to identify public key certificate to verify the JWT token also helps if you have more than one key
used on your OAuth 2.0 provider. One use case is that you must support at least two keys during key
rotation on a yearly basis due to maximize the security. 


## JwtMockHandler

This is a testing OAuth2 endpoints provider and it can be injected into the handler chain for unit 
testing so that it won't depend on an instance of light-oauth2 for unit tests. It can be used by other
frameworks and also services developed on top of these frameworks. 

## Long lived token

To make integration test easier, a long lived token is provided by the oauth2 
server and it can be found at https://github.com/networknt/light-oauth2 README.md

## Configuration

Here is the default security.yml

```yaml
# Security configuration in light-rest-4j framework.
---
# Enable JWT verification flag.
enableVerifyJwt: true

# Enable JWT scope verification. Only valid when enableVerifyJwt is true.
enableVerifyScope: true

# User for test only. should be always be false on official environment.
enableMockJwt: false

# JWT signature public certificates. kid and certificate path mappings.
jwt:
  certificate:
    '100': oauth/primary.crt
    '101': oauth/secondary.crt
  clockSkewInSeconds: 60

# Enable or disable JWT token logging
logJwtToken: true

# Enable or disable client_id, user_id and scope logging.
logClientUserScope: false
``` 

### Enable security

To enable security, just update enableVerifyJwt to true. If you want to verify the scopes defined in
swagger.json against the scope registered on OAuth 2.0 provider, then set enableVerifyScope to true. 
It is highly recommended to have both enabled on production.

### Enable mock JWT generation

If you want to test with different payloads in JWT token or other parameters with JWT token generation,
then you can set enableMockJwt to true; however, you'd better to create a new security.yml and put it
into src/test/resources/config folder so that it works only with your test cases. 


### Public key certificates

The light-4j framework supports multiple public key certificates and they are defined in the jwt section
with kid that mapped to the certificate file in oauth sub folder from config folder. The light-codegen
generates two public key certificates to bootstrap the service startup and you should replace them with
your own certificate for production usage.

### Clock Skew in seconds

This is part of the jwt configuration and default is 60 seconds. It means if the JWT token receives are expired
already but still within 60 seconds from expires time, then it still consider valid as computer clocks might
not synced perfectly and there are network and prcessing latency as well.

### logJwtToken and logClientUserScope

The JWT token is considered sensitive so the customer needs make decision on how the information will be
logged. The logJwtToken will just log the raw token itself and logClientUserScope will log the three most
important piece of info only. Normally, you choose one of them or disable both. It doesn't make sense to
log both as information is duplicated. 


 



