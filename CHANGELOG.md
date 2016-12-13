# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]
### Added

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
- Rename project to light-java
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






