A fast, lightweight and cloud-native microservices framework.

[Stack Overflow](https://stackoverflow.com/questions/tagged/light-4j) |
[Google Group](https://groups.google.com/forum/#!forum/light-4j) |
[Gitter Chat](https://gitter.im/networknt/light-4j) |
[Subreddit](https://www.reddit.com/r/lightapi/) |
[Youtube Channel](https://www.youtube.com/channel/UCHCRMWJVXw8iB7zKxF55Byw) |
[Documentation](https://doc.networknt.com) |
[Contribution Guide](https://doc.networknt.com/contribute/) |

[![Build Status](https://travis-ci.org/networknt/light-4j.svg?branch=master)](https://travis-ci.org/networknt/light-4j) [![codecov.io](https://codecov.io/github/networknt/light-4j/coverage.svg?branch=master)](https://codecov.io/github/networknt/light-4j?branch=master)

## Why call it Light-4J

Light means lightweight, lightning fast and shedding light on how to program with modern Java SE for cloud-native deployment.

## Why am I doing it

I had been working on the Java EE platforms since early 2000 and suffered the performance and productivity issues. In 2014, I realized that the IT industry was moving from Monolithic to Microservices and from on-premise data centers to the public clouds.

To reduce the production cost for my applications, I need to find a lightweight platform that has a small memory footprint,  high throughput, and low latency in Java. Java EE and Spring/Spring Boot are too heavy to be considered. Other lightweight Java platforms all have different issues on the separation between the business logic and technical cross-cutting concerns. Moreover, they are not cloud-native or designed for container/cloud.

Without finding anything suitable, I started to build my open-source platform light-4j on top of the Undertow HTTP core by following the [principles](https://doc.networknt.com/about/principles/). Because my approach is very radical, and it is against products from big corporations, I have been attacked by some entities online and offline constantly. However, I am so convinced that my approach is the future direction and spend all my effort to continue the development. Today, a lot of companies are using light-4j on production with very positive feedback. It further encourages me to devote myself to the framework and to extend the platform to build an ecosystem.

For users who benefit from the platform, you can [become a sponsor](https://github.com/sponsors/stevehu) for me so that I can put all my effort into the open-source projects.

## Why use this framework

### Fast and small memory footprint to lower production cost.

It is 44 times faster than the most popular microservices platform Spring Boot embedded Tomcat and use only 1/5 of memory. Here are the [benchmark](https://github.com/networknt/microservices-framework-benchmark) results compare with Spring Boot and other microservices frameworks. Here is the third party [comparison](https://www.techempower.com/benchmarks/) with other Web frameworks.

### Provide an embedded gateway to address cross-cutting concerns.
* Plugin architecture for startup/shutdown hooks and middleware components
* Distributed OAuth2 JWT security verification as part of the framework
* Request and response validation against OpenAPI specification at runtime
* Metrics collected in Influxdb/Prometheus and viewed from Grafana Dashboard for both services and clients
* Global exception handling for runtime exception, API exception, and other checked exceptions
* Mask sensitive data like the credit card, sin number, etc. before logging
* Sanitize cross-site scripting for query parameters, request headers and body
* Audit to dump important info or entire request and response.
* Body parser to support different content types
* Standardized response code and messages from the configuration file
* Externalized configuration for all modules for the dockerized environment
* CORS pre-flight handler for SPA (Angular or React) from another domain
* Rate limiting for services that exposed outside to the Internet
* Service registry and discovery support direct, Consul and Zookeeper
* Client-side discovery and load balance to eliminate proxies
* A client module that is tightly integrated with Light-OAuth2 and supports traceability

### Design and Test driven development to increase productivity
Design OpenAPI specification and generate the service from it. The specification is also
part of the framework to drive security verification and request validation at runtime.

Unit/End-to-End test stubs are generated to enable test driven approach for quality product.

Debugging within IDE just like standalone application for better developer productivity.

### Built-in DevOps flow to support continuous integration to production

Dockerfile and DevOps supporting files are generated to support dockerization and continuous
integration to production.

### Multiple frameworks for different type of microservices

- [light-rest-4j](https://github.com/networknt/light-rest-4j) is a RESTful microservice framework with OpenAPI specification for code generation and runtime security and validation
- [light-graphql-4j](https://github.com/networknt/light-graphql-4j) is a GraphQL microservice framework that supports schema generation from IDL and plugin.
- [light-hybrid-4j](https://github.com/networknt/light-hybrid-4j) is a hybrid microservice framework that takes advantages of both monolithic and microservice architectures.
- [light-eventuate](https://github.com/networknt/light-eventuate-4j) is a messaging based microservice framework based on Kafka, event sourcing and CQRS

### Multiple languages support

All the open sourced frameworks are built in Java and we are working on Nodejs framework internally.
In the future, we might provide Golang framework as well and all them are sharing the same eco-system
and market place.


### OAuth2 server, portal and services to form ecosystem

[OAuth2 Server](https://github.com/networknt/light-oauth2) for security and [Portal](https://github.com/networknt/light-portal)
for production monitor and management. The portal is also a marketplace to link clients and services
together.


## Getting Started

There are two ways to start your project:

### Light-codegen generator

You can use [light-codegen](https://github.com/networknt/light-codegen) to generate a working project.
Currently, it supports light-rest-4j, light-graphql-4j, light-hybrid-server-4j and light-hybrid-service-4j.
light-eventuate code generator is coming.

The light-codegen project README.md describes four ways to use the generator with examples.

* Clone and build the light-codgen and use the codegen-cli command line utility
* Use docker image networknt/light-codegen to run the codegen-cli command line utility
* Use generate.sh from [model-config](https://github.com/networknt/model-config) repo to generate projects based on conventions.
* Generate code from web site with codegen-web API. (API is ready but UI needs to be built)


### Starting from an example project

The other way to start your project is to copy from [light-example-4j](https://github.com/networknt/light-example-4j).

You can find the description of these [examples](https://www.networknt.com/example/)

Also, there are some [tutorials](https://www.networknt.com/tutorial/)


## Debugging

[To run/debug from IDE](https://www.networknt.com/tutorial/common/debug/), you need to
configure a Java application with main class "com.networknt.server.Server" and working
directory is your project folder. There is no container and you are working on just a standalone
Java application.


## Start Server

### In IDE
create a Java application that main class is com.networknt.server.Server and working
directory is your project root folder. You can debug your server just like a POJO application.

### From Maven

mvn exec:exec

### Command Line

```
java -jar target/demo-0.1.0.jar
```

## Stop Server

you can use Ctrl+C to kill the server but for production use the following command

```
kill -s TERM <pid>
```

The server has a shutdown hook and the above command allow it to clean up. For example,
complete in-flight requests and close the database connections etc. If service registry
and discovery is used, then the server will send shutdown event to service registry and
keep processing requests for 30 seconds until all clients refreshes their local cache
before shutting down.

## Appreciation

- Light-4j has been optimized by using open source license of [JProfiler](http://www.ej-technologies.com/products/jprofiler/overview.html)
from [ej-technologies](http://www.ej-technologies.com/).

## License

Light-4j and all light-*-4j frameworks are available under the Apache 2.0 license. See the [LICENSE](https://www.apache.org/licenses/LICENSE-2.0)
file for more info.


### Jackson 3 development migration

This development line uses Jackson 3.2.3 with `tools.jackson` package names and
Maven coordinates. Jackson annotations remain under
`com.fasterxml.jackson.annotation`. Public APIs exposing Jackson types are
source and binary incompatible with the `2.x` maintenance branch. Versions are
unchanged during implementation; a next-major version and coordinated release
are required before artifact publication.

Install the migrated upstream snapshots and lower light-4j modules in an
isolated Maven repository, then install migrated http-client, then build the
whole light-4j reactor with the same repository. The rule-loader uses the
migrated yaml-rule mapper and Jackson 3 TypeReference. JSONPath startup hooks
use Jackson3JsonProvider and Jackson3MappingProvider.

Framework JSON mapper policies and the accepted ClientConfig binding difference
are documented in [config/README.md](config/README.md). The http-client helper
has a separate timestamp date policy and accepted built-in Optional support;
see its migration README. Jackson 3 mappers are immutable: derive a private
mapper from a builder rather than modifying the shared mapper.

Jackson parse failures are unchecked. Handled request/configuration failures
retain their existing error paths, including HTTP 400 for malformed JSON in
BodyHandler. The header and certificate deserializers retain legacy string and
structured input forms through Jackson 3 deserialization contexts.

Config YAML continues through the existing Engine loader. The approved
Jackson YAML resolver changes in yaml-rule/json-overlay are separate from the
JSON compatibility profile. Classic SnakeYAML pins remain until the plan's
final dependency audit; this migration does not remove third-party YAML needs.


To verify the coupled light-4j/http-client migration without cached networknt
artifacts, use the source bootstrap script:

```sh
JACKSON3_MAVEN_REPO=/path/to/dedicated-maven-repository \
JACKSON3_YAML_RULE=/path/to/migrated-yaml-rule \
JACKSON3_HTTP_CLIENT=/path/to/migrated-http-client \
./scripts/verify-jackson3-bootstrap.sh
```

The script refuses a repository containing `com/networknt`, builds yaml-rule,
installs lower light-4j modules, tests and installs http-client, then tests the
whole light-4j reactor and installs it. Cached third-party dependencies/plugins
may be seeded first; set `JACKSON3_MAVEN_OFFLINE=1` to verify offline. All Maven
writes use the selected repository. Merge the two migration PRs as a coordinated
unit; publishing coordinated next-major artifacts and verifying their resolution
from a separate cache remain release gates.

The Jackson 3 yaml-rule dependency is `4.0.0-SNAPSHOT`; published `3.0.1` uses
Jackson 2. The source-build workflow resolves an open prerequisite PR to its
latest head and follows its base branch once merged. Already merged prerequisites
use master. Each run logs the exact checked-out commits and uses a fresh Maven
repository. Rerun downstream source checks after prerequisite changes and before
merging; a prior green run qualifies only the logged commits. The workflows also
support manual dispatch. For downstream consumers, also set
`JACKSON3_JSON_OVERLAY`, `JACKSON3_OPENAPI_PARSER`, and `JACKSON3_CONSUMER` to
source directories. The script tests and installs both parser prerequisites,
then runs the complete consumer test reactor. This verifies source builds;
published-artifact resolution remains a release gate.

The Phase 7 consumers `light-gateway`, `http-sidecar`, and `yaml-rule-plugin`
still require the yaml-rule coordinate update and their Jackson 3 build gates.
Their old yaml-rule versions resolve Jackson 2. They are not yet qualified
against this development line; do not release them with these snapshots before
their planned migration.

HTTP fixtures dispatch sends to their connection's owning I/O thread, as required
by Undertow's non-thread-safe `ClientConnection` contract. The contract is present
in both inspected Undertow 2.3.22.Final and 2.4.1.Final sources.
`Http2ClientTest.testFixtureRequestCallbackRunsOnOwningIoThread` verifies dispatch,
callback ownership, and response delivery. This does not establish why earlier
off-thread fixtures began failing, or qualify production callers:
[issue #2817](https://github.com/networknt/light-4j/issues/2817) tracks the direct
production sends, including the asynchronous `callService` paths.
[Issue #2816](https://github.com/networknt/light-4j/issues/2816) separately tracks
the named rate-limit configuration resetting the limiter on each request.

ExternalServiceHandler uses blocking Java HTTP calls and must run on a worker
thread. The checked-in light-gateway `config/external-gateway` and
`product/external-access-point` chains place ExceptionHandler first; its
`isInIoThread()/dispatch(this)` guard moves initial chain execution to workers.
RequestInterceptorInjectionHandler can later resume the chain on an I/O thread
after an asynchronous body read, so initial dispatch is not a universal safeguard.
Worker-thread execution is required at the actual external-service entry. The
single-I/O-thread ExternalServiceHandlerTest fixture uses that actual framework
handler and asserts worker execution for synchronous entry; it does not cover
the asynchronous continuation. Matched external requests now check their actual
thread and dispatch only forwarding to workers before rewriting. Configuration,
routing, and request timeouts are resolved once and passed to forwarding; dispatch
does not re-enter routing or write shared timeout fields. The streaming
ExternalServiceWorkerDispatchTest forces read-listener continuation and verifies
that an unrelated request progresses while the upstream is blocked; it also
checks body preservation, one rewrite, direct GETs, and unchanged unmatched routing.
The worker continuation currently constructs a new default ExceptionHandler per
dispatched request. It returns the default structured error response (ERR10011 /
HTTP 400 for an uncaught IOException) after the original exception-handler frame
has returned. This does not reuse the chain's configured handler or its enabled
flag: the dispatched path applies default exception handling even when the chain
intentionally disables it. Other worker-entry paths still use their surrounding
chain policy. This is an accepted limitation of this fix, tracked in
[issue #2820](https://github.com/networknt/light-4j/issues/2820) for separate
exception-module continuation support and review. The current implementation
retains ingress-proxy's compile dependency on the exception module.
Regressions compare
worker-entry and streamed-continuation failures, cover 405 rejection and metrics
injection, and check that concurrent initialization shares one HTTP client.
Client initialization uses a volatile reference and a double-checked guard;
established clients and network sends do not acquire the initialization lock.
BodyHandler alone is insufficient: its dispatch depends on a Content-Type header.
Dispatch protects I/O threads; blocking upstream calls still consume the bounded
worker pool. These tests do not establish worker-pool saturation capacity, add
backpressure, or provide an overload policy. Size worker capacity and upstream
timeouts for the deployment. SalesforceHandler and MrasHandler also use blocking
HTTP sends and need separate execution-chain qualification; this guard covers
ExternalServiceHandler only. These source tests do not qualify deployed handler
chains.

Prerequisites are resolved to full commit SHAs before checkout. To replay a
failed dependency combination, manually dispatch the source-build workflow with
its uploaded `source-refs.json` contents as the `source_refs` input. Omitted
repositories resolve their current source head; supplied values must be full
commit SHAs. Each green result qualifies only the recorded combination.

GitHub manual dispatch requires this workflow to exist on the default branch.
Before merge, use the recorded commits to reproduce with local source checkouts.
