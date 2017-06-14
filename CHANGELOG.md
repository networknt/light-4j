# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]
### Added

### Changed

## 1.3.3 - 2017-06-14
### Added

### Changed
- fixes #71 break the metrics tie to security for client_id

## 1.3.2 - 2017-06-14
### Added

### Changed
- Fixes #58 add filter and role based auth handler
- Fixes #66 update keystore and truststore for both client and server
- Fixes #67 add serviceId to slf4j MDC and remove from audit.yml
- Fixes #68 add log statement in influxdb reporter with counter size
- Fixes #70 make default constructor of UnsafeLongAdderImpl public

## 1.3.1 - 2017-06-03
### Added

### Changed
- Fixes #60 add java doc and update online documents
- Fixes #61 response time and status code are not shown up in audit.log
- Fixes #63 rename HealthHandler to HealthGetHandler in order to inject from light-codegen


## 1.3.0 - 2017-05-05
### Added
- Fixes #59 change project name to light-4j

## 1.2.8 - 2017-05-02
### Added
- Fixes #42 upgrade dependencies
- Fixes #43 add more debug info during metrics startup
- Fixes #48 clean up status.xml and make comment on each segment

## 1.2.7 - 2017-03-28
### Added
- Fixes #38 add status codes and utilities for light-graphql-4j

### Changed
- Fixes #34 enable HTTP/2 on server and make HTTP/2 configurable in server.yml
- Fixes #36 upgrade to undertow 1.4.11

## 1.2.6 - 2017-03-17
### Added
- Fixes #33 to create a separate secret.yml for Kubernetes integration

### Changed
- Fixes #30 check if token is null and ignore it for API to API call
- Define static LIGHT_JAVA_CONFIG_DIR to avoid hard-coded property name
- Fixes #32 support yaml and yml config format
- 
## 1.2.5 - 2017-03-03
### Added

### Changed
- Fixes #11 inject server info into swagger-codegen
- Change server name to L to optimize performance

## 1.2.4 - 2017-02-19
### Added
- Fixes #27 Add rate limiting module

### Changed
- Upgrade to Undertow 1.4.10
- Fixes #26 Update CORS handler to support method level control 

## 1.2.3 - 2017-02-08
### Added

### Changed
- Fixes some google error-prone warnings
- Fixes #25 Add a system property in Server.java to redirect Undertow logs to slf4j
- Fixes #25 ExceptionHandler to dispatch in the beginning to by pass Connectors in Undertow

## 1.2.2 - 2017-02-03
### Added
- Add Cluster module to work with Client module for service discovery and load balance
- Add Https support in server module. 

### Changed
- Update consul registry to make it simpler and streamline.
- Update cluster to work with registrator and docker


## 1.2.1 - 2017-01-25
### Added

### Changed
- Clean up maven dependencies


## 1.2.0 - 2017-01-22
### Added
- Add service registry and discovery
- Add zookeeper support for resgistry and discovery
- Add consul support for registry and discovery
- Add direct registry for direct ip and port registry and discovery
- Add Cors handler to support OAuth2 login page from another domain

### Changed
- Resolve Maven Eclipse errors
- Enhanced service module to support parameterized constructor
- Integrate server with registry
- Spin off swagger, security and validator handlers to light-rest-4j

## 1.1.7 - 2017-01-14
### Added
- Add error status for OAuth2 server

### Changed
- Fix warnings reported from google error-prone

## 1.1.7 - 2017-01-09
### Added
- Add error status for OAuth2 server

### Changed
- Update JwtHelper to throw JoseException instead of Exception

## 1.1.6 - 2017-01-01
### Added
- Add error status for OAuth2 server
- Add local first load balancer (Thanks @ddobrin)

### Changed
- Remove duplicated plugin
- Update JwtHelper to support JWT token generation with parameters

## 1.1.5 - 2016-12-24
### Added
- Add HashUtil for password used in OAuth2 server light-oauth2
- Add Status code for OAuth2 server

### Changed

## 1.1.4 - 2016-12-12
### Added
- Switcher is a module to control on/off of certain component during runtime
- Discovery is a module used by Client to discover services by names from Consul or ZooKeeper
- Registry is a module used by Server to register itself to Consul or ZooKeeper during server startup 

### Changed
- Update service module to support spaces in interface key part.
- Integrate Google Error-prone for static source code scan
- Fixed some many warnings from Intellij Idea Inspect

## 1.1.3 - 2016-12-03
### Added

### Changed
- Service config changed to JSON format so that properties can be injected 

## 1.1.2 - 2016-11-28
### Added

### Changed
- Add support to one interface to map multiple implementations in singleton service factory
- Add support to multiple interfaces to map multiple implementations in singleton service factory

## 1.1.1 - 2016-11-26
### Added
- Add a method to get framework version from /META-INFO/maven/com.networknt/info/pom.properties
- Add a missing handler error code in status.json
- Add MDC for correlation id in logging
- Add service module which is a singleton service factory

### Changed
- Disable metrics by default in metrics module so that it won't try to report to influxdb

## 1.1.0 - 2016-11-06
### Added
- Traceability handler to set traceabilityId to the response header from request header
- Correlation handler to generate correlationId if it doesn't exist in the request header
- Create dump handler but not implemented yet

### Changed
- Update Client to support traceabilityId and correlationId propagation
- Refactor Audit to split full dump out.
- Refactor Audit handler to output into audit log
- Update Swagger Handler to add endpoint into the request header for audit log
- Update JwtVerifyHandler to put scope_client_id into the request header


## 1.0.2 - 2016-11-03
### Added

### Changed
- Upgrade to json-schema-form 0.1.3
- Fixes #2 exchange completed already issue.


## 1.0.1 - 2016-10-30
### Added
- Sanitizer middleware to handle cross site scripting on header and body of request.
- Health endpoint to indicate if the server is alive by sending OK.

### Changed
- Update body middleware so that it checks content type and parse the application/json to list or map.
- Update Validator middleware to handle body as object or string
- Move info to package info instead of audit
- Upgrade undertow to 1.4.4-Final to address patch issue.
- Update default server.json to bind to 0.0.0.0 instead of localhost for docker

## 1.0.0 - 2016-10-21

### Added
- Add startup hook provider to allow API project to initialize resource, loading spring app context etc.
- Add shutdown hook provider to allow API project to release connection pools or other resources.
- Add Dockerfile to the root of generated API project.

### Changed
- Rename project to light-4j
- JsonPath configuration is done in a startup hook provider now.

## 0.1.9 - 2016-10-17
### Added
- Add metrics with Influx DB and Grafana combination.

### Changed
- Move swagger.json from swagger module resources to test resources so that petstore specification won't be included in API project by default.
- Security scope verification will check swagger object before enabling it.
- Validator test now has petstore swagger.json in test resources.
- Update docs

## 0.1.8 - 2016-10-10
### Added
- Add new Status code into status.json for OAuth2 server
- Add docs folder for project documentation

### Changed
- Fix a bug in client to prevent calling oauth2 server multiple times if in renew window.
- Add socket timeout for client
- update client.json in test resource to simulate different errors.

## 0.1.7 - 2016-10-05
### Added
- Introduce MiddlewareHandler interface so that user can plug in more middleware or switch middleware

### Changed
- Fixed info test case to clear the injected config
- All middleware handlers implement MiddlewareHandler interface so that they are loaded from SPI
- Middleware handlers will be enabled by checking isEnable()
- Update validator test case to remove oauth2 dependency
- Fix the NPE issue if swagger specification does not have security defined
- SwaggerHandler will only be enabled if swagger.json exists in config
- Fix the token helper to get token from OAuth2 server

## 0.1.6 - 2016-10-02
### Added
- Add header parameter validation against swagger specification

### Changed
- Refactor validator to fail fast while error occurs
- All validator returns Status object instead


## 0.1.5 - 2016-10-01
### Added
- A body parser handler to parse body into String and attached to the exchange
- A swagger handler to identify operation based on request uri and method and attach to the exchange

### Changed
- Move swagger related classes from utility to swagger module
- Update security module to leverage swagger handler attachment
- Update validator module to leverage swagger handler and body handler attachments
- Server Info handler is not injected in server but is included into swagger specification

## 0.1.4 - 2016-09-29
### Added
- A generic exception handler for runtime exception, ApiException and uncaught exception

### Changed
- Move checked exceptions to exception module from status

## 0.1.3 - 2016-09-28
### Added

### Changed
- Fix a NPE in request validator if query parameter is missing
- Do not validate query parameter if there is none.
- Handle the case that security definition is empty.


## 0.1.2 - 2016-09-27
### Added
- Jwt token scope verification based on swagger spec in security
- Status module to standardize error response
- Config can be loaded from classpath directly so that test case can inject different combination of configs.

### Changed
- Update the jwt.json and security.json to support multiple certificates and kid to pick up the right certificate
for jwt verification. Also, expired token will throw ExpiredJwtException now.
- Move request uri matching and swagger.json to utility
- Move exceptions to status from utility
- Instead of using Jackson ObjectMapper to serialize Status object, using toString now. 10 times faster


## 0.1.1 - 2016-09-19
### Added
- Audit
- Client
- Info
- Mask
- Validator



## 0.1.0 - 2016-08-16
### Added
- Server
- Config
- Utility
- Security






