# Change Log

## [1.5.10](https://github.com/networknt/light-4j/tree/1.5.10) (2018-03-02)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.9...1.5.10)

**Closed issues:**

- add subject\_claims and access\_claims constants [\#170](https://github.com/networknt/light-4j/issues/170)
- Port enabling indicator [\#168](https://github.com/networknt/light-4j/issues/168)
- add prometheus to the module list in parent pom.xml [\#164](https://github.com/networknt/light-4j/issues/164)

**Merged pull requests:**

- prometheus metrics package and class name change [\#166](https://github.com/networknt/light-4j/pull/166) ([chenyan71](https://github.com/chenyan71))
- get latest develop branch [\#163](https://github.com/networknt/light-4j/pull/163) ([chenyan71](https://github.com/chenyan71))

## [1.5.9](https://github.com/networknt/light-4j/tree/1.5.9) (2018-02-21)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.8...1.5.9)

**Closed issues:**

- Support dynamic port binding for Kubernetes hostNetwork [\#162](https://github.com/networknt/light-4j/issues/162)
- switch light-config-server connection to HTTP 2.0 [\#159](https://github.com/networknt/light-4j/issues/159)

**Merged pull requests:**

- update to latest Develop branch [\#160](https://github.com/networknt/light-4j/pull/160) ([chenyan71](https://github.com/chenyan71))

## [1.5.8](https://github.com/networknt/light-4j/tree/1.5.8) (2018-02-03)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.7...1.5.8)

**Closed issues:**

- fix a typo in variable JwT\_CLOCK\_SKEW\_IN\_SECONDS [\#158](https://github.com/networknt/light-4j/issues/158)
- accept other optional fields in OAuth2 token response [\#156](https://github.com/networknt/light-4j/issues/156)
- handle a list of string instead of list of maps in BodyHandler [\#154](https://github.com/networknt/light-4j/issues/154)
- Add a status code ERR12042 SERVICE\_ENDPOINT\_NOT\_FOUND [\#153](https://github.com/networknt/light-4j/issues/153)

**Merged pull requests:**

- Fix HTTP verbs [\#152](https://github.com/networknt/light-4j/pull/152) ([morganseznec](https://github.com/morganseznec))

## [1.5.7](https://github.com/networknt/light-4j/tree/1.5.7) (2018-01-09)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.6...1.5.7)

**Implemented enhancements:**

- provide a utility to substitute environment variables in config  [\#149](https://github.com/networknt/light-4j/issues/149)

**Closed issues:**

- change secret.yml loading from SecretConfig to Map for flexibility [\#151](https://github.com/networknt/light-4j/issues/151)
- Remove docs folder and repo specific document site once all contents are migrated [\#139](https://github.com/networknt/light-4j/issues/139)

## [1.5.6](https://github.com/networknt/light-4j/tree/1.5.6) (2017-12-29)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.5...1.5.6)

**Fixed bugs:**

- Get token un Http2Client line 367 is hard coded using Http2 [\#146](https://github.com/networknt/light-4j/issues/146)

**Closed issues:**

- some compiler warnings  [\#148](https://github.com/networknt/light-4j/issues/148)
- Add email sender module and update secret.yml [\#147](https://github.com/networknt/light-4j/issues/147)
- Add host header for getkey in oauthHelper for HTTP 1.1 [\#145](https://github.com/networknt/light-4j/issues/145)

## [1.5.5](https://github.com/networknt/light-4j/tree/1.5.5) (2017-12-15)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.4...1.5.5)

**Implemented enhancements:**

- make it configurable to  get jwt public certificates from cache or from oauth server  [\#140](https://github.com/networknt/light-4j/issues/140)

**Closed issues:**

- Find a way to propagate callback exception to the caller thread in Http2Client [\#144](https://github.com/networknt/light-4j/issues/144)
- Encrypt the values in secret.yml [\#143](https://github.com/networknt/light-4j/issues/143)
- Add two more callback functions in Http2Client to return response time [\#142](https://github.com/networknt/light-4j/issues/142)
- Adding java bean initializer and manual injection test cases for service module [\#141](https://github.com/networknt/light-4j/issues/141)
- Add support for two or more beans initialized by one initializer class and method [\#138](https://github.com/networknt/light-4j/issues/138)
- Add an API to manipulate SingletonServiceFactory to add new entries programatically [\#137](https://github.com/networknt/light-4j/issues/137)
- Loading Java bean with initializer class and method in service module [\#136](https://github.com/networknt/light-4j/issues/136)
- Reduce token endpoint access timeout to 4 seconds [\#135](https://github.com/networknt/light-4j/issues/135)
- Remove e.printStackTrace\(\) in the source code [\#134](https://github.com/networknt/light-4j/issues/134)

## [1.5.4](https://github.com/networknt/light-4j/tree/1.5.4) (2017-11-21)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.3...1.5.4)

**Fixed bugs:**

- Need to ensure that server lookup returns null if there is no entry defined in service.yml [\#132](https://github.com/networknt/light-4j/issues/132)

## [1.5.3](https://github.com/networknt/light-4j/tree/1.5.3) (2017-11-20)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.2...1.5.3)

**Fixed bugs:**

- Fixed the class cast exception in service module if getting an array but only one impl is configured [\#131](https://github.com/networknt/light-4j/issues/131)

## [1.5.2](https://github.com/networknt/light-4j/tree/1.5.2) (2017-11-20)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.1...1.5.2)

**Implemented enhancements:**

- Add a new constant OPENAPI\_OPERATION\_STRING for OpenAPI 3.0 support [\#130](https://github.com/networknt/light-4j/issues/130)
- Improve configurability for networknt.handler.MiddlewareHandler [\#129](https://github.com/networknt/light-4j/issues/129)
- Add support for interface with generic type in service module [\#127](https://github.com/networknt/light-4j/issues/127)
- Update serviceMap key to interface class name instead of class in service module [\#126](https://github.com/networknt/light-4j/issues/126)

**Fixed bugs:**

- Need to check certain section is empty for header.yml in HeaderHandler [\#125](https://github.com/networknt/light-4j/issues/125)

## [1.5.1](https://github.com/networknt/light-4j/tree/1.5.1) (2017-11-09)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.0...1.5.1)

**Implemented enhancements:**

- Upgrade to Undertow 1.4.20.Final [\#108](https://github.com/networknt/light-4j/issues/108)
- Implement rpc and websocket support with light-java-rpc [\#6](https://github.com/networknt/light-4j/issues/6)
- Server module should only initialize client instance if config server is enabled [\#119](https://github.com/networknt/light-4j/issues/119)
- Make Jwt token verification cache configurable. [\#113](https://github.com/networknt/light-4j/issues/113)

**Closed issues:**

- Remove mockito dependencies in most modules [\#124](https://github.com/networknt/light-4j/issues/124)
- Add pre-release script to update versions [\#123](https://github.com/networknt/light-4j/issues/123)
- Customize return of the status from cross-cutting concerns [\#122](https://github.com/networknt/light-4j/issues/122)
- Add url debug output in ConsulClientImpl [\#121](https://github.com/networknt/light-4j/issues/121)
- Split integration tests from unit tests [\#120](https://github.com/networknt/light-4j/issues/120)

## [1.5.0](https://github.com/networknt/light-4j/tree/1.5.0) (2017-10-21)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.4.6...1.5.0)

**Implemented enhancements:**

- Add SecretConfig to model properties in secret.yml instead of Map.  [\#117](https://github.com/networknt/light-4j/issues/117)
- Change server registry and client discovery to support environment tag for test segregation [\#116](https://github.com/networknt/light-4j/issues/116)
- Add utilities classes for light-workflow-4j [\#114](https://github.com/networknt/light-4j/issues/114)
- Remove some dependencies as they are not being used or can be eliminated [\#112](https://github.com/networknt/light-4j/issues/112)
- Utility methods in server project for accessing path variables [\#104](https://github.com/networknt/light-4j/issues/104)

**Closed issues:**

- Add a header handler to manipulate request header based on the config file [\#118](https://github.com/networknt/light-4j/issues/118)
- Upgrade dependency versions for commons lang + deprecate commons-codec [\#106](https://github.com/networknt/light-4j/issues/106)
- Implement session manager that can support web server cluster. [\#73](https://github.com/networknt/light-4j/issues/73)

**Merged pull requests:**

- Upgraded Undertow, Added Utility Interfaces for Header, path, query extraction [\#111](https://github.com/networknt/light-4j/pull/111) ([sachinwalia2k8](https://github.com/sachinwalia2k8))
- Resolved \#1 Upgrading libraries and other changes [\#107](https://github.com/networknt/light-4j/pull/107) ([sachinwalia2k8](https://github.com/sachinwalia2k8))

## [1.4.6](https://github.com/networknt/light-4j/tree/1.4.6) (2017-09-22)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.4.5...1.4.6)

## [1.4.5](https://github.com/networknt/light-4j/tree/1.4.5) (2017-09-22)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.4.4...1.4.5)

**Closed issues:**

- Return invalid json error in body handler for malformed body [\#102](https://github.com/networknt/light-4j/issues/102)

## [1.4.4](https://github.com/networknt/light-4j/tree/1.4.4) (2017-09-21)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.4.3...1.4.4)

**Implemented enhancements:**

- service.yml configuration need to have entry name that is unique in order to support two or more db connection pools [\#50](https://github.com/networknt/light-4j/issues/50)
- Centralize configurations to light-config-server which is backed by git repositories [\#29](https://github.com/networknt/light-4j/issues/29)
- Revisit client module for the per route connection pooling in config in cluster/load balance/discovery environment. [\#24](https://github.com/networknt/light-4j/issues/24)
- \[service module\] Add support for multiple definitions and bean references [\#9](https://github.com/networknt/light-4j/issues/9)

**Closed issues:**

- Update BodyHandler to parse the body and then put the stream back for subsequent handlers [\#101](https://github.com/networknt/light-4j/issues/101)
- Add a reverse proxy middleware handler [\#100](https://github.com/networknt/light-4j/issues/100)

## [1.4.3](https://github.com/networknt/light-4j/tree/1.4.3) (2017-09-10)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.4.2...1.4.3)

**Closed issues:**

- Calling consul directly with Http2Client instead of consul client [\#94](https://github.com/networknt/light-4j/issues/94)

## [1.4.2](https://github.com/networknt/light-4j/tree/1.4.2) (2017-08-31)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.4.1...1.4.2)

**Merged pull requests:**

- Update SingletonServiceFactory.java [\#99](https://github.com/networknt/light-4j/pull/99) ([ruslanys](https://github.com/ruslanys))

## [1.4.1](https://github.com/networknt/light-4j/tree/1.4.1) (2017-08-30)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.4.0...1.4.1)

**Fixed bugs:**

- Timeout when looking up from consul with ecwid client [\#19](https://github.com/networknt/light-4j/issues/19)

**Closed issues:**

- Upgrade to undertow 1.4.19.Final as there an HTTP 2.0 bug in 1.4.18.Final [\#98](https://github.com/networknt/light-4j/issues/98)
- Add host head when sending requests to influxdb in InfluxDbHttpSender [\#97](https://github.com/networknt/light-4j/issues/97)
- Add enableHttp2 in client.yml to control if Http2Client will be using HTTP 2.0 protocol [\#96](https://github.com/networknt/light-4j/issues/96)
- Refactor server info component object so that it is easy to be consumed by api-certification [\#95](https://github.com/networknt/light-4j/issues/95)
- update default client.yml to remove the settings for http1.1 [\#93](https://github.com/networknt/light-4j/issues/93)
- Create a websocket example in light-example-4j to demo how to use websocket endpoint. [\#64](https://github.com/networknt/light-4j/issues/64)

**Merged pull requests:**

- Update README.md [\#92](https://github.com/networknt/light-4j/pull/92) ([joaozitopolo](https://github.com/joaozitopolo))
- Update SingletonServiceFactory.java [\#91](https://github.com/networknt/light-4j/pull/91) ([ruslanys](https://github.com/ruslanys))

## [1.4.0](https://github.com/networknt/light-4j/tree/1.4.0) (2017-08-22)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.3.5...1.4.0)

**Implemented enhancements:**

- Register JavaTimeModule for the default Jackson ObjectMapper from Config [\#82](https://github.com/networknt/light-4j/issues/82)

**Closed issues:**

- Remove Client which depends on apache httpclient and replace with Http2Client [\#90](https://github.com/networknt/light-4j/issues/90)
- gzip compression  [\#88](https://github.com/networknt/light-4j/issues/88)
- Upgrade to undertow 1.4.18.Final to support http2 [\#87](https://github.com/networknt/light-4j/issues/87)
- Complete the Http2Client in client module [\#85](https://github.com/networknt/light-4j/issues/85)
- Dynamically load public key certificate from OAuth2 provider [\#84](https://github.com/networknt/light-4j/issues/84)

**Merged pull requests:**

- Config module: Added 2 extra test cases for LocalDateTime and LocalDate [\#83](https://github.com/networknt/light-4j/pull/83) ([pragmaticway](https://github.com/pragmaticway))

## [1.3.5](https://github.com/networknt/light-4j/tree/1.3.5) (2017-08-01)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.3.4...1.3.5)

**Closed issues:**

- Remove jsoniter dependencies in mask until it has a feature of jsonpath [\#81](https://github.com/networknt/light-4j/issues/81)
- Update validatePassword in HashUtil to accept origianlPassword as char\[\] instead of String [\#80](https://github.com/networknt/light-4j/issues/80)
- Add TLS certificate and OAuth2 certificate SHA1 fingerprint to the /server/info output [\#79](https://github.com/networknt/light-4j/issues/79)
- Resolve security issues reported from Fortify scanner [\#78](https://github.com/networknt/light-4j/issues/78)
- Remove JsonPath in Mask module with JsonIter to simplify dependencies [\#77](https://github.com/networknt/light-4j/issues/77)
- Add getTempDir for NioUtils and add test cases for zip file manipulation [\#76](https://github.com/networknt/light-4j/issues/76)

## [1.3.4](https://github.com/networknt/light-4j/tree/1.3.4) (2017-07-08)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.3.3...1.3.4)

**Implemented enhancements:**

- test: use random port [\#55](https://github.com/networknt/light-4j/issues/55)
- JWT token verification with cached token and expire time [\#47](https://github.com/networknt/light-4j/issues/47)
- Need to encrypt secrets in config files so that they won't be leaked [\#31](https://github.com/networknt/light-4j/issues/31)

**Closed issues:**

- InetAddress is not working in Docker for Mac as hostname is not mapped to /etc/hosts [\#75](https://github.com/networknt/light-4j/issues/75)
- Update ConsulRegistry to refactor discovery cache to one layer from two [\#74](https://github.com/networknt/light-4j/issues/74)
- Add CodeVerifierUtil to support PKCE in light-oauth2 implementation [\#72](https://github.com/networknt/light-4j/issues/72)

## [1.3.3](https://github.com/networknt/light-4j/tree/1.3.3) (2017-06-14)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.3.2...1.3.3)

**Closed issues:**

- Break the metrics tie to security for client\_id [\#71](https://github.com/networknt/light-4j/issues/71)

## [1.3.2](https://github.com/networknt/light-4j/tree/1.3.2) (2017-06-14)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.3.1...1.3.2)

**Implemented enhancements:**

- Request and response filter implementation [\#58](https://github.com/networknt/light-4j/issues/58)

**Closed issues:**

- Make UnsafeLongAdderImpl default construction public [\#70](https://github.com/networknt/light-4j/issues/70)
- Switch to AuditInfo attachment for metrics collection [\#69](https://github.com/networknt/light-4j/issues/69)
- Add debug level log when InfluxDb reporter is call with counter size. [\#68](https://github.com/networknt/light-4j/issues/68)
- Add serviceId sId into slf4j MDC so that it can be added to all logging statement along with cId [\#67](https://github.com/networknt/light-4j/issues/67)
- Switch to undertow server and client truststore and keystore [\#66](https://github.com/networknt/light-4j/issues/66)
- microservices sample error [\#65](https://github.com/networknt/light-4j/issues/65)
- Any plan to create tools like "jhispter" to help build microservices? [\#5](https://github.com/networknt/light-4j/issues/5)

## [1.3.1](https://github.com/networknt/light-4j/tree/1.3.1) (2017-06-03)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.3.0...1.3.1)

**Implemented enhancements:**

- Put more comments in source code to improve the readability [\#60](https://github.com/networknt/light-4j/issues/60)

**Fixed bugs:**

- Response time and status code is not shown up in audit log [\#61](https://github.com/networknt/light-4j/issues/61)

**Closed issues:**

- Update HealthHandler to HealthGetHandler in order to inject into the light-rest-4j generator in light-codegen [\#63](https://github.com/networknt/light-4j/issues/63)
- Upgrade json-schema-validator to 0.1.7 from 0.1.5 [\#62](https://github.com/networknt/light-4j/issues/62)

## [1.3.0](https://github.com/networknt/light-4j/tree/1.3.0) (2017-05-06)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.2.8...1.3.0)

**Closed issues:**

- Change the project name to light-4j instead of light-java as java is a trademark of Oracle [\#59](https://github.com/networknt/light-4j/issues/59)

## [1.2.8](https://github.com/networknt/light-4j/tree/1.2.8) (2017-05-02)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.2.7...1.2.8)

**Implemented enhancements:**

- Clean up status.yml and add comments for each segment [\#48](https://github.com/networknt/light-4j/issues/48)
- Add more debug info to metrics as people having trouble to config it right with Influxdb [\#43](https://github.com/networknt/light-4j/issues/43)
- Upgrade dependencies to the latest version [\#42](https://github.com/networknt/light-4j/issues/42)

**Fixed bugs:**

- Client - propagateHeaders expect JWT token to be in the request header or output an error.  [\#41](https://github.com/networknt/light-4j/issues/41)

**Closed issues:**

- Create a NioUtils with some helpers in utility module [\#54](https://github.com/networknt/light-4j/issues/54)
- Add NIO utility and status code to support light-codegen [\#52](https://github.com/networknt/light-4j/issues/52)
- Adding kid to the header of the JWT token issued by light-java. [\#46](https://github.com/networknt/light-4j/issues/46)
- Bump up scope mismatch log from debug to warn as it is security violation [\#45](https://github.com/networknt/light-4j/issues/45)

## [1.2.7](https://github.com/networknt/light-4j/tree/1.2.7) (2017-03-28)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.2.6...1.2.7)

**Implemented enhancements:**

- Add status code and a util method for light-java-graphql [\#38](https://github.com/networknt/light-4j/issues/38)
- Separate secrets from config files in order to support Kubernetes secrets and configmap [\#33](https://github.com/networknt/light-4j/issues/33)

**Closed issues:**

- Upgrade to undertow 1.4.11.Final [\#36](https://github.com/networknt/light-4j/issues/36)
- Implement GraphQL support with light-java-graphql [\#8](https://github.com/networknt/light-4j/issues/8)

## [1.2.6](https://github.com/networknt/light-4j/tree/1.2.6) (2017-03-18)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.2.5...1.2.6)

**Implemented enhancements:**

- Token scope and spec scope mismatch error is not clear in logs [\#35](https://github.com/networknt/light-4j/issues/35)
- Switch server and client to HTTP/2 [\#34](https://github.com/networknt/light-4j/issues/34)
- Config file support yaml format along with json [\#32](https://github.com/networknt/light-4j/issues/32)

**Closed issues:**

- NullPointerException when populating a request to call another API due to original token is missing [\#30](https://github.com/networknt/light-4j/issues/30)

## [1.2.5](https://github.com/networknt/light-4j/tree/1.2.5) (2017-03-04)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.2.4...1.2.5)

**Implemented enhancements:**

- Find the best location to inject server info to the routing handler [\#11](https://github.com/networknt/light-4j/issues/11)

**Closed issues:**

- Alternate Config impls ... S3, http? [\#28](https://github.com/networknt/light-4j/issues/28)

## [1.2.4](https://github.com/networknt/light-4j/tree/1.2.4) (2017-02-20)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.2.3...1.2.4)

**Implemented enhancements:**

- Add rate limit in order to prevent DDOS attack for public facing services [\#27](https://github.com/networknt/light-4j/issues/27)
- Allow cors handler to specify which method is allowed. [\#26](https://github.com/networknt/light-4j/issues/26)

## [1.2.3](https://github.com/networknt/light-4j/tree/1.2.3) (2017-02-09)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.2.2...1.2.3)

**Implemented enhancements:**

- Add a system property to redirect jboss logs to slf4j [\#25](https://github.com/networknt/light-4j/issues/25)

**Closed issues:**

- Add TLS support for server module [\#22](https://github.com/networknt/light-4j/issues/22)
- Add cluster module to work with client module for service discovery and load balance [\#21](https://github.com/networknt/light-4j/issues/21)

## [1.2.2](https://github.com/networknt/light-4j/tree/1.2.2) (2017-02-04)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.2.1...1.2.2)

## [1.2.1](https://github.com/networknt/light-4j/tree/1.2.1) (2017-01-25)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.2.0...1.2.1)

## [1.2.0](https://github.com/networknt/light-4j/tree/1.2.0) (2017-01-22)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.1.7...1.2.0)

**Implemented enhancements:**

- Integrate service registry and discovery with Server and Client modules [\#17](https://github.com/networknt/light-4j/issues/17)
- Implement a CORS handler to support calls from Single Page Application from another domain [\#14](https://github.com/networknt/light-4j/issues/14)
- Update JwtHelper to throw JoseException instead of throwing Exception [\#12](https://github.com/networknt/light-4j/issues/12)
- Implement service registry and discovery that support consul and zookeeper [\#10](https://github.com/networknt/light-4j/issues/10)

**Closed issues:**

- Spin off swagger, validator and security to light-java-rest repo [\#18](https://github.com/networknt/light-4j/issues/18)
- service - support parameterized constructor instead of default constructor and then set properties [\#16](https://github.com/networknt/light-4j/issues/16)

**Merged pull requests:**

- Resolve Maven Eclipse errors [\#15](https://github.com/networknt/light-4j/pull/15) ([ddobrin](https://github.com/ddobrin))

## [1.1.7](https://github.com/networknt/light-4j/tree/1.1.7) (2017-01-08)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.1.6...1.1.7)

## [1.1.6](https://github.com/networknt/light-4j/tree/1.1.6) (2017-01-02)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.1.5...1.1.6)

**Merged pull requests:**

- Add a load balancer matching across the local host, then round-robin [\#7](https://github.com/networknt/light-4j/pull/7) ([ddobrin](https://github.com/ddobrin))

## [1.1.5](https://github.com/networknt/light-4j/tree/1.1.5) (2016-12-24)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.1.4...1.1.5)

## [1.1.4](https://github.com/networknt/light-4j/tree/1.1.4) (2016-12-13)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.1.3...1.1.4)

## [1.1.3](https://github.com/networknt/light-4j/tree/1.1.3) (2016-12-03)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.1.2...1.1.3)

## [1.1.2](https://github.com/networknt/light-4j/tree/1.1.2) (2016-11-29)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.1.1...1.1.2)

## [1.1.1](https://github.com/networknt/light-4j/tree/1.1.1) (2016-11-27)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.1.0...1.1.1)

**Merged pull requests:**

- Docs: Fix switched links [\#4](https://github.com/networknt/light-4j/pull/4) ([spinscale](https://github.com/spinscale))
- Fixing small issue with documentation's example. \(Changing "clone ...… [\#3](https://github.com/networknt/light-4j/pull/3) ([lkoolma](https://github.com/lkoolma))

## [1.1.0](https://github.com/networknt/light-4j/tree/1.1.0) (2016-11-07)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.0.2...1.1.0)

## [1.0.2](https://github.com/networknt/light-4j/tree/1.0.2) (2016-11-04)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.0.1...1.0.2)

**Fixed bugs:**

- Exchange already complete in jwt token if scope is mismatched [\#2](https://github.com/networknt/light-4j/issues/2)

## [1.0.1](https://github.com/networknt/light-4j/tree/1.0.1) (2016-10-30)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.0.0...1.0.1)

## [1.0.0](https://github.com/networknt/light-4j/tree/1.0.0) (2016-10-21)
[Full Changelog](https://github.com/networknt/light-4j/compare/0.1.9...1.0.0)

## [0.1.9](https://github.com/networknt/light-4j/tree/0.1.9) (2016-10-18)
[Full Changelog](https://github.com/networknt/light-4j/compare/0.1.8...0.1.9)

## [0.1.8](https://github.com/networknt/light-4j/tree/0.1.8) (2016-10-11)
[Full Changelog](https://github.com/networknt/light-4j/compare/0.1.7...0.1.8)

## [0.1.7](https://github.com/networknt/light-4j/tree/0.1.7) (2016-10-05)
[Full Changelog](https://github.com/networknt/light-4j/compare/0.1.6...0.1.7)

## [0.1.6](https://github.com/networknt/light-4j/tree/0.1.6) (2016-10-02)
[Full Changelog](https://github.com/networknt/light-4j/compare/0.1.5...0.1.6)

## [0.1.5](https://github.com/networknt/light-4j/tree/0.1.5) (2016-10-01)
[Full Changelog](https://github.com/networknt/light-4j/compare/0.1.4...0.1.5)

## [0.1.4](https://github.com/networknt/light-4j/tree/0.1.4) (2016-09-30)
[Full Changelog](https://github.com/networknt/light-4j/compare/0.1.3...0.1.4)

## [0.1.3](https://github.com/networknt/light-4j/tree/0.1.3) (2016-09-28)
[Full Changelog](https://github.com/networknt/light-4j/compare/0.1.2...0.1.3)

## [0.1.2](https://github.com/networknt/light-4j/tree/0.1.2) (2016-09-25)
[Full Changelog](https://github.com/networknt/light-4j/compare/0.1.1...0.1.2)

## [0.1.1](https://github.com/networknt/light-4j/tree/0.1.1) (2016-09-18)


\* *This Change Log was automatically generated by [github_changelog_generator](https://github.com/skywinder/Github-Changelog-Generator)*