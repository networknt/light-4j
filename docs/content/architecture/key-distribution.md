---
date: 2017-09-19T21:22:47-04:00
title: JWT Public Key Certification Distribution
---

light-4j and related frameworks integrate with light-oauth2 very closely so that tokens
issued by light-oauth2 can be verified on services built on top of light-*-4j frameworks
independently. We call this centralized security policy management on light-oauth2 and
distributed policy enforcement on service. This eliminate the needs to call oauth2 server 
to verify token for each request to a service. If we do that, then the oauth2 server will 
become a bottleneck as the number of calls between services are significantly higher then 
traditional web services architecture.

In order to do token verification distributedly, we have to use PKI to sign the JWT token
and distribute the public key certificate to each running service. Each service will use
the public key certificate to verify token signature to ensure that the token is issued 
by the right OAuth 2.0 provider and the claims are not tempered.

In traditional web service architecture, the public key certificate normally will be copied
to the target host which the services are deployed during first deployment and then copied
the new public key certificate once it is rotated. This is doable because we know the IP
address of the service and we can locate the exact host to deploy the certificate. However,
in pure microservices architecture, all services are in Docker container and orchestrated
by Kubernetes. You don't have the exact address of each service and one service can be down
on one VM and be started on another VM the next seconds. The traditional way to push the
certificate to service is not an option anymore. 

The solution we use is to pull the certificate from light-oauth2 by services whenever 
necessary. 


## Bootstrap certificates

When a service is generated from light-codegen, it will include two public key certificates
for testing. These will be replaced once you move to an official testing environment within
your organization as you services will be using an official testing oauth2 server with a
purchased key pair. When the service goes to production, the production certificate will be
packaged to the server in order to bootstrap the service on production. Basically, this will
ensure that all services will be ready to verify JWT tokens immediately.

## Which key to use

As mentioned above, light-4j supports multiple public key certificates at anytime because
once key is rotated, service still need to use the old key to verify tokens sign by the old
private key as these tokens were issued before the new key is deployed and they are not
expired yet. 

Here is the security.yml config file for security handler.

```yaml
# Security configuration in light framework.
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

As you can see the certificate configuration is a map between kid and the key filename. When
light-oauth2 server issues a token, it will put the kid into the header of the token. When
service receives a token, it will check the kid first and then lookup the certificate map
to figure out which certificate is needed to verify the token. The certificates are loaded
during server startup so the lookup and verification should be really fast. 

## Rotate certificate

Once the key changed on the light-oauth2 server, it will be using the new key to sign the
token and the new token will have a new kid for example "102". Once the service receives
the token it checked the local key map and cannot find the key with kid = 102, then it will
call light-oauth2 key service with kid=102 in the path. Once the key is downloaded to the
local, it will be cached for subsequent token verifications. 

Here is the document for [key distribution](https://networknt.github.io/light-oauth2/services/key/) 
service in light-oauth2.

 



