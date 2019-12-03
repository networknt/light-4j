# Change Log

## [2.0.8](https://github.com/networknt/light-4j/tree/2.0.8) (2019-11-27)


**Merged pull requests:**


- fixes \#623 make the getDefaultXnioSsl public from Http2Client as ligh… [\#624](https://github.com/networknt/light-4j/pull/624) ([stevehu](https://github.com/stevehu))
- fixes \#620 [\#621](https://github.com/networknt/light-4j/pull/621) ([chenyan71](https://github.com/chenyan71))
## [2.0.7](https://github.com/networknt/light-4j/tree/2.0.7) (2019-10-26)


**Merged pull requests:**


- Fix/Replace - to _ to match the OpenShift environment variable syntax [\#618](https://github.com/networknt/light-4j/pull/618) ([jiachen1120](https://github.com/jiachen1120))
- fixes \#613 service throws UT005001 once the metrics/influxdb module i… [\#614](https://github.com/networknt/light-4j/pull/614) ([stevehu](https://github.com/stevehu))
## [2.0.6](https://github.com/networknt/light-4j/tree/2.0.6) (2019-09-13)


**Merged pull requests:**


- fixed \#609 [\#610](https://github.com/networknt/light-4j/pull/610) ([chenyan71](https://github.com/chenyan71))
## [2.0.5](https://github.com/networknt/light-4j/tree/2.0.5) (2019-08-30)


**Merged pull requests:**


- fixes \#605 server exits without any error in the console [\#606](https://github.com/networknt/light-4j/pull/606) ([stevehu](https://github.com/stevehu))
- fixes \#603 add JwtVerifier to replace JwtHelper [\#604](https://github.com/networknt/light-4j/pull/604) ([stevehu](https://github.com/stevehu))


## [2.0.4](https://github.com/networknt/light-4j/tree/2.0.4) (2019-08-16)


**Merged pull requests:**


- fixes \#509 -DskipTests does not work to skip unit tests for master [\#602](https://github.com/networknt/light-4j/pull/602) ([stevehu](https://github.com/stevehu))
- Fix/\#590 skip tests [\#591](https://github.com/networknt/light-4j/pull/591) ([BalloonWen](https://github.com/BalloonWen))
- fixes \#600 upgrade jackson-databind to 2.9.9.3 [\#601](https://github.com/networknt/light-4j/pull/601) ([stevehu](https://github.com/stevehu))
## [2.0.3](https://github.com/networknt/light-4j/tree/2.0.3) (2019-07-31)


**Merged pull requests:**


- fixes \#596 useJson default to false to ensure backward compatibility [\#597](https://github.com/networknt/light-4j/pull/597) ([stevehu](https://github.com/stevehu))
- API-189: Return Json format result from HealthGetHandler [\#589](https://github.com/networknt/light-4j/pull/589) ([jsu216](https://github.com/jsu216))
- fixes \#592 add a debug statement to output the discovered url in Oaut… [\#593](https://github.com/networknt/light-4j/pull/593) ([stevehu](https://github.com/stevehu))
- fixes \#586 FormData.FormValue cannot be handled by Jackson JSON parser [\#587](https://github.com/networknt/light-4j/pull/587) ([stevehu](https://github.com/stevehu))
- fixes \#583 Success result is returned even light-oauth2 returns an er… [\#585](https://github.com/networknt/light-4j/pull/585) ([stevehu](https://github.com/stevehu))
- fixes \#578 to use a hashset to track used ports [\#582](https://github.com/networknt/light-4j/pull/582) ([stevehu](https://github.com/stevehu))
- fixes \#579 when all instances are down and restarted, the client disc… [\#580](https://github.com/networknt/light-4j/pull/580) ([stevehu](https://github.com/stevehu))
- randomly pick up port number for dynamic registry [\#578](https://github.com/networknt/light-4j/pull/578) ([stevehu](https://github.com/stevehu))
- Fix/\#573 consul registry notify [\#577](https://github.com/networknt/light-4j/pull/577) ([BalloonWen](https://github.com/BalloonWen))
- Fix/\#547 deprecate apis - client module [\#561](https://github.com/networknt/light-4j/pull/561) ([BalloonWen](https://github.com/BalloonWen))
- Fix/npe service to url [\#576](https://github.com/networknt/light-4j/pull/576) ([jiachen1120](https://github.com/jiachen1120))


## [2.0.2](https://github.com/networknt/light-4j/tree/2.0.2) (2019-07-10)


**Merged pull requests:**


- add Prometheus hotspot monitor [\#567](https://github.com/networknt/light-4j/pull/567) ([chenyan71](https://github.com/chenyan71))
- 514 sanitizer new config properties [\#563](https://github.com/networknt/light-4j/pull/563) ([jefperito](https://github.com/jefperito))
- Add OpenTracing support for observability [\#549](https://github.com/networknt/light-4j/pull/549) ([stevehu](https://github.com/stevehu))
- reverted some values of client.yml for testing [\#558](https://github.com/networknt/light-4j/pull/558) ([BalloonWen](https://github.com/BalloonWen))
- fixes \#559 update default consulUrl to http instead of https [\#560](https://github.com/networknt/light-4j/pull/560) ([stevehu](https://github.com/stevehu))
- fixes \#556 remove dependency on secret.yml from server and email modules [\#557](https://github.com/networknt/light-4j/pull/557) ([stevehu](https://github.com/stevehu))
- fixes \#554 move the consul token to the consul.yml from the secret.yml [\#555](https://github.com/networknt/light-4j/pull/555) ([stevehu](https://github.com/stevehu))
- fixes \#552 get client_secret from client.yml instead of secret.yml [\#553](https://github.com/networknt/light-4j/pull/553) ([stevehu](https://github.com/stevehu))
- fixes \#550 loopback address is used when register to Consul from a st… [\#551](https://github.com/networknt/light-4j/pull/551) ([stevehu](https://github.com/stevehu))
- Fix/\#519 keystore fall back [\#525](https://github.com/networknt/light-4j/pull/525) ([jiachen1120](https://github.com/jiachen1120))
- Feat/\#502 consul integrate test [\#503](https://github.com/networknt/light-4j/pull/503) ([jiachen1120](https://github.com/jiachen1120))
- fixes \#539 sync jdk11 branch to 1.6.x to ensure code similarity [\#540](https://github.com/networknt/light-4j/pull/540) ([stevehu](https://github.com/stevehu))
## [2.0.1](https://github.com/networknt/light-4j/tree/2.0.1) (2019-06-13)

**Merged pull requests:**

- Add JsonWebKey support in JWT key verification [\#511](https://github.com/networknt/light-4j/pull/511) ([jsu216](https://github.com/jsu216))
- Fix/\#512 config overwritten [\#516](https://github.com/networknt/light-4j/pull/516) ([jiachen1120](https://github.com/jiachen1120))
- fixes \#513 disable sanitizer handler by default [\#515](https://github.com/networknt/light-4j/pull/515) ([stevehu](https://github.com/stevehu))
- Fix/\#504 read keystore from system property [\#505](https://github.com/networknt/light-4j/pull/505) ([jiachen1120](https://github.com/jiachen1120))
- fixes \#508 associate the correlationId with the traceabilityId when c… [\#509](https://github.com/networknt/light-4j/pull/509) ([stevehu](https://github.com/stevehu))
- Feat/default decryptor [\#501](https://github.com/networknt/light-4j/pull/501) ([jiachen1120](https://github.com/jiachen1120))
- fixes \#499 ConsulRegistry discoverService returns http always [\#500](https://github.com/networknt/light-4j/pull/500) ([stevehu](https://github.com/stevehu))
- fixes \#497 implement service discovery in OauthHelper [\#498](https://github.com/networknt/light-4j/pull/498) ([stevehu](https://github.com/stevehu))
- fix path separator - \#483 [\#484](https://github.com/networknt/light-4j/pull/484) ([dz-1](https://github.com/dz-1))
- fixes \#495 support multiple key servers for token and sign [\#496](https://github.com/networknt/light-4j/pull/496) ([stevehu](https://github.com/stevehu))
- fixes \#493 update timeoutCount to AtomicInteger in circuit breaker [\#494](https://github.com/networknt/light-4j/pull/494) ([stevehu](https://github.com/stevehu))
- Fix/\#491 direct registry with env [\#492](https://github.com/networknt/light-4j/pull/492) ([jiachen1120](https://github.com/jiachen1120))
- \#240 timeout feature \#241 circuit breaker [\#485](https://github.com/networknt/light-4j/pull/485) ([jefperito](https://github.com/jefperito))
- Fix/\#482 consul service discovery caching [\#486](https://github.com/networknt/light-4j/pull/486) ([BalloonWen](https://github.com/BalloonWen))
- fixes \#489 need to rollback the lazy init of config in Server [\#490](https://github.com/networknt/light-4j/pull/490) ([stevehu](https://github.com/stevehu))
- fixes \#487 resolve test case failure in the server module [\#488](https://github.com/networknt/light-4j/pull/488) ([stevehu](https://github.com/stevehu))
- fixes \#474 register light-hybrid-4j services in Server module [\#475](https://github.com/networknt/light-4j/pull/475) ([stevehu](https://github.com/stevehu))
- fixes issue \#480 [\#481](https://github.com/networknt/light-4j/pull/481) ([chenyan71](https://github.com/chenyan71))
- fixes \#478 service getting unregistered from consul after a while [\#479](https://github.com/networknt/light-4j/pull/479) ([stevehu](https://github.com/stevehu))
- Fixed the scope caching error [\#477](https://github.com/networknt/light-4j/pull/477) ([jiachen1120](https://github.com/jiachen1120))
- fixes \#468 resolve backward compatible issue for server.config [\#469](https://github.com/networknt/light-4j/pull/469) ([stevehu](https://github.com/stevehu))
- fixes \#466 Incorrect status code [\#467](https://github.com/networknt/light-4j/pull/467) ([stevehu](https://github.com/stevehu))
- fixes \#464 update license and copyright headers [\#465](https://github.com/networknt/light-4j/pull/465) ([stevehu](https://github.com/stevehu))
- Pluggable config server feature RFC\#0019 [\#451](https://github.com/networknt/light-4j/pull/451) ([santoshaherkar](https://github.com/santoshaherkar))
- Added new server option ALLOW_UNESCAPED_CHARACTERS_IN_URL [\#462](https://github.com/networknt/light-4j/pull/462) ([jiachen1120](https://github.com/jiachen1120))
-  populate path params - \#light-rest-4j/issues/67 [\#463](https://github.com/networknt/light-4j/pull/463) ([dz-1](https://github.com/dz-1))
- fixes \#460 upgrade maven plugins to the latest versions [\#461](https://github.com/networknt/light-4j/pull/461) ([stevehu](https://github.com/stevehu))
- fixes \#456 resolve warnnings from errorprone [\#457](https://github.com/networknt/light-4j/pull/457) ([stevehu](https://github.com/stevehu))
- fixes \#454 add test cases for AESDecryptor and DecryptConstructor [\#455](https://github.com/networknt/light-4j/pull/455) ([stevehu](https://github.com/stevehu))
- fixed caching issue when initlizing multiple token request havenot been tested [\#452](https://github.com/networknt/light-4j/pull/452) ([BalloonWen](https://github.com/BalloonWen))
- Feat/\#351 server options configurable [\#432](https://github.com/networknt/light-4j/pull/432) ([jiachen1120](https://github.com/jiachen1120))
- Feat/\#427 config decryption [\#429](https://github.com/networknt/light-4j/pull/429) ([jiachen1120](https://github.com/jiachen1120))
- Feat/light 4j/\#391 support caching multiple Jwts [\#410](https://github.com/networknt/light-4j/pull/410) ([BalloonWen](https://github.com/BalloonWen))
- Cache unparsed request body (configurable/ for application/json request) [\#447](https://github.com/networknt/light-4j/pull/447) ([jiachen1120](https://github.com/jiachen1120))
- fixes \#448 remove broken import statements in Http2Client [\#449](https://github.com/networknt/light-4j/pull/449) ([stevehu](https://github.com/stevehu))
- move classes in undertow package to networknet packages - \#433 [\#435](https://github.com/networknt/light-4j/pull/435) ([dz-1](https://github.com/dz-1))
- Integrate decryption module in all retrievals of config - \#413 [\#414](https://github.com/networknt/light-4j/pull/414) ([dz-1](https://github.com/dz-1))
- fixes \#445 update client module to access signing service [\#446](https://github.com/networknt/light-4j/pull/446) ([stevehu](https://github.com/stevehu))
- fixes \#443 Add a new constant in http header for service_url [\#444](https://github.com/networknt/light-4j/pull/444) ([stevehu](https://github.com/stevehu))
- fixes \#441 detect copy/paste code with PMD 6.12.0 [\#442](https://github.com/networknt/light-4j/pull/442) ([stevehu](https://github.com/stevehu))
- fixes \#438 add NOTICE.txt for all third party dependencies and licenses [\#439](https://github.com/networknt/light-4j/pull/439) ([stevehu](https://github.com/stevehu))
- fixes \#434 update copyright and license in source code [\#436](https://github.com/networknt/light-4j/pull/436) ([stevehu](https://github.com/stevehu))
- fixes \#430 add status code to status.yml for light-codegen [\#431](https://github.com/networknt/light-4j/pull/431) ([stevehu](https://github.com/stevehu))
- just improvements [\#419](https://github.com/networknt/light-4j/pull/419) ([jefperito](https://github.com/jefperito))

## [1.6.2](https://github.com/networknt/light-4j/tree/1.6.2) (2019-05-17)


**Merged pull requests:**


- Add JsonWebKey support in JWT key verification [\#511](https://github.com/networknt/light-4j/pull/511) ([jsu216](https://github.com/jsu216))
- Fix/\#512 config overwritten [\#516](https://github.com/networknt/light-4j/pull/516) ([jiachen1120](https://github.com/jiachen1120))
- fixes \#513 disable sanitizer handler by default [\#515](https://github.com/networknt/light-4j/pull/515) ([stevehu](https://github.com/stevehu))
- Fix/\#504 read keystore from system property [\#505](https://github.com/networknt/light-4j/pull/505) ([jiachen1120](https://github.com/jiachen1120))
- fixes \#508 associate the correlationId with the traceabilityId when c… [\#509](https://github.com/networknt/light-4j/pull/509) ([stevehu](https://github.com/stevehu))
## [1.6.1](https://github.com/networknt/light-4j/tree/1.6.1) (2019-05-03)


**Merged pull requests:**


- Feat/default decryptor [\#501](https://github.com/networknt/light-4j/pull/501) ([jiachen1120](https://github.com/jiachen1120))
- fixes \#499 ConsulRegistry discoverService returns http always [\#500](https://github.com/networknt/light-4j/pull/500) ([stevehu](https://github.com/stevehu))
- fixes \#497 implement service discovery in OauthHelper [\#498](https://github.com/networknt/light-4j/pull/498) ([stevehu](https://github.com/stevehu))
- fix path separator - \#483 [\#484](https://github.com/networknt/light-4j/pull/484) ([dz-1](https://github.com/dz-1))
- fixes \#495 support multiple key servers for token and sign [\#496](https://github.com/networknt/light-4j/pull/496) ([stevehu](https://github.com/stevehu))
- fixes \#493 update timeoutCount to AtomicInteger in circuit breaker [\#494](https://github.com/networknt/light-4j/pull/494) ([stevehu](https://github.com/stevehu))
- Fix/\#491 direct registry with env [\#492](https://github.com/networknt/light-4j/pull/492) ([jiachen1120](https://github.com/jiachen1120))
- \#240 timeout feature \#241 circuit breaker [\#485](https://github.com/networknt/light-4j/pull/485) ([jefperito](https://github.com/jefperito))
- Fix/\#482 consul service discovery caching [\#486](https://github.com/networknt/light-4j/pull/486) ([BalloonWen](https://github.com/BalloonWen))
- fixes \#489 need to rollback the lazy init of config in Server [\#490](https://github.com/networknt/light-4j/pull/490) ([stevehu](https://github.com/stevehu))
- fixes \#487 resolve test case failure in the server module [\#488](https://github.com/networknt/light-4j/pull/488) ([stevehu](https://github.com/stevehu))
- fixes \#474 register light-hybrid-4j services in Server module [\#475](https://github.com/networknt/light-4j/pull/475) ([stevehu](https://github.com/stevehu))
- fixes issue \#480 [\#481](https://github.com/networknt/light-4j/pull/481) ([chenyan71](https://github.com/chenyan71))
- fixes \#478 service getting unregistered from consul after a while [\#479](https://github.com/networknt/light-4j/pull/479) ([stevehu](https://github.com/stevehu))
- Fixed the scope caching error [\#477](https://github.com/networknt/light-4j/pull/477) ([jiachen1120](https://github.com/jiachen1120))
- fixes \#468 resolve backward compatible issue for server.config [\#469](https://github.com/networknt/light-4j/pull/469) ([stevehu](https://github.com/stevehu))
- fixes \#466 Incorrect status code [\#467](https://github.com/networknt/light-4j/pull/467) ([stevehu](https://github.com/stevehu))
- fixes \#464 update license and copyright headers [\#465](https://github.com/networknt/light-4j/pull/465) ([stevehu](https://github.com/stevehu))
- Pluggable config server feature RFC\#0019 [\#451](https://github.com/networknt/light-4j/pull/451) ([santoshaherkar](https://github.com/santoshaherkar))
- Added new server option ALLOW_UNESCAPED_CHARACTERS_IN_URL [\#462](https://github.com/networknt/light-4j/pull/462) ([jiachen1120](https://github.com/jiachen1120))
-  populate path params - \#light-rest-4j/issues/67 [\#463](https://github.com/networknt/light-4j/pull/463) ([dz-1](https://github.com/dz-1))
- fixes \#460 upgrade maven plugins to the latest versions [\#461](https://github.com/networknt/light-4j/pull/461) ([stevehu](https://github.com/stevehu))
- fixes \#456 resolve warnnings from errorprone [\#457](https://github.com/networknt/light-4j/pull/457) ([stevehu](https://github.com/stevehu))
## [1.6.0](https://github.com/networknt/light-4j/tree/1.6.0) (2019-04-05)


**Merged pull requests:**


- fixes \#454 add test cases for AESDecryptor and DecryptConstructor [\#455](https://github.com/networknt/light-4j/pull/455) ([stevehu](https://github.com/stevehu))
- fixed caching issue when initlizing multiple token request havenot been tested [\#452](https://github.com/networknt/light-4j/pull/452) ([BalloonWen](https://github.com/BalloonWen))
- Feat/\#351 server options configurable [\#432](https://github.com/networknt/light-4j/pull/432) ([jiachen1120](https://github.com/jiachen1120))
- Feat/\#427 config decryption [\#429](https://github.com/networknt/light-4j/pull/429) ([jiachen1120](https://github.com/jiachen1120))
- Feat/light 4j/\#391 support caching multiple Jwts [\#410](https://github.com/networknt/light-4j/pull/410) ([BalloonWen](https://github.com/BalloonWen))
- Cache unparsed request body (configurable/ for application/json request) [\#447](https://github.com/networknt/light-4j/pull/447) ([jiachen1120](https://github.com/jiachen1120))
- fixes \#448 remove broken import statements in Http2Client [\#449](https://github.com/networknt/light-4j/pull/449) ([stevehu](https://github.com/stevehu))
- move classes in undertow package to networknet packages - \#433 [\#435](https://github.com/networknt/light-4j/pull/435) ([dz-1](https://github.com/dz-1))
- Integrate decryption module in all retrievals of config - \#413 [\#414](https://github.com/networknt/light-4j/pull/414) ([dz-1](https://github.com/dz-1))
- fixes \#445 update client module to access signing service [\#446](https://github.com/networknt/light-4j/pull/446) ([stevehu](https://github.com/stevehu))
- fixes \#443 Add a new constant in http header for service_url [\#444](https://github.com/networknt/light-4j/pull/444) ([stevehu](https://github.com/stevehu))
- fixes \#441 detect copy/paste code with PMD 6.12.0 [\#442](https://github.com/networknt/light-4j/pull/442) ([stevehu](https://github.com/stevehu))
- fixes \#438 add NOTICE.txt for all third party dependencies and licenses [\#439](https://github.com/networknt/light-4j/pull/439) ([stevehu](https://github.com/stevehu))
- fixes \#434 update copyright and license in source code [\#436](https://github.com/networknt/light-4j/pull/436) ([stevehu](https://github.com/stevehu))
- fixes \#430 add status code to status.yml for light-codegen [\#431](https://github.com/networknt/light-4j/pull/431) ([stevehu](https://github.com/stevehu))
- just improvements [\#419](https://github.com/networknt/light-4j/pull/419) ([jefperito](https://github.com/jefperito))
- Feat/light 4j/\#420 fixed TTL check [\#428](https://github.com/networknt/light-4j/pull/428) ([BalloonWen](https://github.com/BalloonWen))
## [1.5.32](https://github.com/networknt/light-4j/tree/1.5.32) (2019-03-19)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.31...1.5.32)

**Implemented enhancements:**

- original status.yml info in light-4j is overwritten by customer, which will lose errors for light-4j [\#389](https://github.com/networknt/light-4j/issues/389)

**Fixed bugs:**

- Dynamic Port binding not working as expected in light4j [\#415](https://github.com/networknt/light-4j/issues/415)
- Error happen if there is not value in values.yml for services.yml injection [\#407](https://github.com/networknt/light-4j/issues/407)

**Closed issues:**

- add last handler to Handler class for handler chain inject in the beginning [\#421](https://github.com/networknt/light-4j/issues/421)
- setup Travis cron to run integration test daily [\#405](https://github.com/networknt/light-4j/issues/405)

**Merged pull requests:**

- Fix/\#425 status merge [\#426](https://github.com/networknt/light-4j/pull/426) ([jiachen1120](https://github.com/jiachen1120))
- accept defaultGroupKey in Http2Client.SSL - \#423 [\#424](https://github.com/networknt/light-4j/pull/424) ([dz-1](https://github.com/dz-1))
- fixes \#421 add last handler to Handler class for handler chain inject… [\#422](https://github.com/networknt/light-4j/pull/422) ([stevehu](https://github.com/stevehu))
- Fix/\#415 dynamic port binding [\#417](https://github.com/networknt/light-4j/pull/417) ([jiachen1120](https://github.com/jiachen1120))

## [2.0.0](https://github.com/networknt/light-4j/tree/2.0.0) (2019-03-24)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.32...2.0.0)

**Implemented enhancements:**

- Add conditional handler in the handler chain [\#300](https://github.com/networknt/light-4j/issues/300)

**Fixed bugs:**

- Fix status merge [\#425](https://github.com/networknt/light-4j/issues/425)
- Feat/light 4j/\#420 fixed TTL check [\#428](https://github.com/networknt/light-4j/pull/428) ([BalloonWen](https://github.com/BalloonWen))

**Closed issues:**

- accept default group key in Http2Client  [\#423](https://github.com/networknt/light-4j/issues/423)

**Merged pull requests:**

- just improvements [\#419](https://github.com/networknt/light-4j/pull/419) ([jefperito](https://github.com/jefperito))

## [1.5.32](https://github.com/networknt/light-4j/tree/1.5.32) (2019-03-19)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.31...1.5.32)

**Implemented enhancements:**

- original status.yml info in light-4j is overwritten by customer, which will lose errors for light-4j [\#389](https://github.com/networknt/light-4j/issues/389)

**Fixed bugs:**

- Dynamic Port binding not working as expected in light4j [\#415](https://github.com/networknt/light-4j/issues/415)
- Error happen if there is not value in values.yml for services.yml injection [\#407](https://github.com/networknt/light-4j/issues/407)

**Closed issues:**

- add last handler to Handler class for handler chain inject in the beginning [\#421](https://github.com/networknt/light-4j/issues/421)
- setup Travis cron to run integration test daily [\#405](https://github.com/networknt/light-4j/issues/405)

**Merged pull requests:**

- Fix/\#425 status merge [\#426](https://github.com/networknt/light-4j/pull/426) ([jiachen1120](https://github.com/jiachen1120))
- accept defaultGroupKey in Http2Client.SSL - \#423 [\#424](https://github.com/networknt/light-4j/pull/424) ([dz-1](https://github.com/dz-1))
- fixes \#421 add last handler to Handler class for handler chain inject… [\#422](https://github.com/networknt/light-4j/pull/422) ([stevehu](https://github.com/stevehu))
- Fix/\#415 dynamic port binding [\#417](https://github.com/networknt/light-4j/pull/417) ([jiachen1120](https://github.com/jiachen1120))

## [1.5.31](https://github.com/networknt/light-4j/tree/1.5.31) (2019-03-02)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.30...1.5.31)

**Fixed bugs:**

- Maven build error on eclipse [\#406](https://github.com/networknt/light-4j/issues/406)

**Closed issues:**

- SSL error - client module - release 1.5.29 [\#398](https://github.com/networknt/light-4j/issues/398)
- Backwards incompatible changes: client module - release 1.5.29 [\#397](https://github.com/networknt/light-4j/issues/397)

**Merged pull requests:**

- Fixing binding to dynamic ports [\#416](https://github.com/networknt/light-4j/pull/416) ([NicholasAzar](https://github.com/NicholasAzar))
- fix/\#407 the value in values.yml can be set to empty [\#409](https://github.com/networknt/light-4j/pull/409) ([jiachen1120](https://github.com/jiachen1120))
- updating iteration of FormData item [\#408](https://github.com/networknt/light-4j/pull/408) ([jefperito](https://github.com/jefperito))
- add code coverage badge [\#404](https://github.com/networknt/light-4j/pull/404) ([lanphan](https://github.com/lanphan))
- Feat/\#389 status merge [\#403](https://github.com/networknt/light-4j/pull/403) ([jiachen1120](https://github.com/jiachen1120))
- add missing license info, remove unused logback setting [\#402](https://github.com/networknt/light-4j/pull/402) ([lanphan](https://github.com/lanphan))
- body: update and add more unittest [\#401](https://github.com/networknt/light-4j/pull/401) ([lanphan](https://github.com/lanphan))

## [1.5.30](https://github.com/networknt/light-4j/tree/1.5.30) (2019-02-21)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.29...1.5.30)

**Implemented enhancements:**

- support a list of config directories in Config module [\#383](https://github.com/networknt/light-4j/issues/383)
- support absolute path for the config file in Config module [\#381](https://github.com/networknt/light-4j/issues/381)
- Allow exclusion list of files which should not check/allow for environment variable setting [\#368](https://github.com/networknt/light-4j/issues/368)
- Environment variable references in the light-4j yaml config files [\#321](https://github.com/networknt/light-4j/issues/321)
- Read configurations from arbitrary directories [\#309](https://github.com/networknt/light-4j/issues/309)

**Closed issues:**

- Config Module - addressing gaps [\#371](https://github.com/networknt/light-4j/issues/371)
- Validation of server identify in a CaaS environment [\#358](https://github.com/networknt/light-4j/issues/358)
- cluster.ServiceToUrl hang sometime [\#303](https://github.com/networknt/light-4j/issues/303)

**Merged pull requests:**

- update basic-auth and some minor update in audit, balance [\#400](https://github.com/networknt/light-4j/pull/400) ([lanphan](https://github.com/lanphan))
- Fixing backwards incompatible interface change in OauthHelper getToken and getTokenFromSaml [\#399](https://github.com/networknt/light-4j/pull/399) ([NicholasAzar](https://github.com/NicholasAzar))

## [1.5.29](https://github.com/networknt/light-4j/tree/1.5.29) (2019-02-16)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.28...1.5.29)

**Implemented enhancements:**

- support flexible config directory in Config module [\#382](https://github.com/networknt/light-4j/issues/382)
- Correlation module: add ability to control the generation of the CorrelationID [\#370](https://github.com/networknt/light-4j/issues/370)
- Feat/\#41 exception handling [\#380](https://github.com/networknt/light-4j/pull/380) ([BalloonWen](https://github.com/BalloonWen))

**Closed issues:**

- Add a default config.yml to the light-4j/Config module [\#394](https://github.com/networknt/light-4j/issues/394)
- output the stacktrace when server registration fails [\#384](https://github.com/networknt/light-4j/issues/384)
- upgrade jsonpath to 2.4.0 and exclude json-smart [\#378](https://github.com/networknt/light-4j/issues/378)
- TechEmpower benchmark [\#369](https://github.com/networknt/light-4j/issues/369)
- Standardize repo line endings on LF [\#365](https://github.com/networknt/light-4j/issues/365)
- Add a new constant string in http header for service url [\#362](https://github.com/networknt/light-4j/issues/362)
- If handler path not implemented/wrong path , it will throw the 404 error code instead of 400. [\#360](https://github.com/networknt/light-4j/issues/360)
- upgrade to undertow 2.0.16.Final [\#356](https://github.com/networknt/light-4j/issues/356)
-  Config module build failing on windows environment [\#354](https://github.com/networknt/light-4j/issues/354)
- Upgrade jackson version to 2.9.8 [\#347](https://github.com/networknt/light-4j/issues/347)
- jdk11 release to maven central failed [\#344](https://github.com/networknt/light-4j/issues/344)
- Performance Optimizations for Max Troughput [\#342](https://github.com/networknt/light-4j/issues/342)
- GraalVM Native Image [\#341](https://github.com/networknt/light-4j/issues/341)
- codegen-cli generated code does not compile - openapi-3 [\#330](https://github.com/networknt/light-4j/issues/330)
- \[question\] - grpc adoption within light4j [\#161](https://github.com/networknt/light-4j/issues/161)
- Warnings with Java 9 [\#103](https://github.com/networknt/light-4j/issues/103)

**Merged pull requests:**

- fix typo, simplify RoundRobinLoadBalanceTest, add license info [\#396](https://github.com/networknt/light-4j/pull/396) ([lanphan](https://github.com/lanphan))
- Fixes \#394 add a default config.yml file [\#395](https://github.com/networknt/light-4j/pull/395) ([ddobrin](https://github.com/ddobrin))
- detail unit test to check content of audit log [\#390](https://github.com/networknt/light-4j/pull/390) ([lanphan](https://github.com/lanphan))
- Feat/\#383 list config directories [\#388](https://github.com/networknt/light-4j/pull/388) ([jiachen1120](https://github.com/jiachen1120))
- Feat/\#381 support absolute path for config file [\#387](https://github.com/networknt/light-4j/pull/387) ([jiachen1120](https://github.com/jiachen1120))
- Feat/\#309 read config from arbitrary directories [\#386](https://github.com/networknt/light-4j/pull/386) ([jiachen1120](https://github.com/jiachen1120))
- fixes \#384 output the stacktrace when server registration fails [\#385](https://github.com/networknt/light-4j/pull/385) ([stevehu](https://github.com/stevehu))
- fixes \#378 upgrade jsonpath to 2.4.0 and exclude json-smart [\#379](https://github.com/networknt/light-4j/pull/379) ([stevehu](https://github.com/stevehu))
- Feat/\#41 exception handling [\#377](https://github.com/networknt/light-4j/pull/377) ([BalloonWen](https://github.com/BalloonWen))
- Fix/config gaps [\#375](https://github.com/networknt/light-4j/pull/375) ([jiachen1120](https://github.com/jiachen1120))
- Fix/error message when fail registration [\#374](https://github.com/networknt/light-4j/pull/374) ([jiachen1120](https://github.com/jiachen1120))
- Allowed exclusion list of files which should not check/allow values injection [\#373](https://github.com/networknt/light-4j/pull/373) ([jiachen1120](https://github.com/jiachen1120))
- Fixing mutual tls on the server side [\#372](https://github.com/networknt/light-4j/pull/372) ([NicholasAzar](https://github.com/NicholasAzar))
- Fix/line endings LF [\#366](https://github.com/networknt/light-4j/pull/366) ([NicholasAzar](https://github.com/NicholasAzar))
- -added an error message in status.yml for validating response content [\#364](https://github.com/networknt/light-4j/pull/364) ([BalloonWen](https://github.com/BalloonWen))
- Fixes \#360 If handler path not implemented/wrong path , it will throw the 404 error code instead of 400. [\#361](https://github.com/networknt/light-4j/pull/361) ([sreenicibc](https://github.com/sreenicibc))
- Validation of server identify in a CaaS environment \#358 [\#359](https://github.com/networknt/light-4j/pull/359) ([dz-1](https://github.com/dz-1))
- fixes \#354 [\#355](https://github.com/networknt/light-4j/pull/355) ([chenyan71](https://github.com/chenyan71))

## [1.5.28](https://github.com/networknt/light-4j/tree/1.5.28) (2019-01-13)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.27...1.5.28)

**Fixed bugs:**

- Config module failing if config file has a key which is not a String [\#353](https://github.com/networknt/light-4j/issues/353)

## [1.5.27](https://github.com/networknt/light-4j/tree/1.5.27) (2019-01-12)
[Full Changelog](https://github.com/networknt/light-4j/compare/2.0.0-BETA2...1.5.27)

**Closed issues:**

- resolve the syntax error in java doc for dump [\#352](https://github.com/networknt/light-4j/issues/352)
- remove the check exception ConfigException [\#350](https://github.com/networknt/light-4j/issues/350)

**Merged pull requests:**

- \#321 Enhanced config injection to support injecting List and Map [\#349](https://github.com/networknt/light-4j/pull/349) ([jiachen1120](https://github.com/jiachen1120))
- refactored dump handler, add mask feature to it [\#345](https://github.com/networknt/light-4j/pull/345) ([BalloonWen](https://github.com/BalloonWen))

## [2.0.0-BETA2](https://github.com/networknt/light-4j/tree/2.0.0-BETA2) (2018-12-29)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.25...2.0.0-BETA2)

**Closed issues:**

- add several network related utilities [\#343](https://github.com/networknt/light-4j/issues/343)
- move JsonMapper from taiji-blockchain to light-4j [\#340](https://github.com/networknt/light-4j/issues/340)

## [1.5.25](https://github.com/networknt/light-4j/tree/1.5.25) (2018-12-24)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.24...1.5.25)

**Closed issues:**

- give wait a default value for backward compatibility [\#339](https://github.com/networknt/light-4j/issues/339)

## [1.5.24](https://github.com/networknt/light-4j/tree/1.5.24) (2018-12-15)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.23...1.5.24)

**Implemented enhancements:**

- Attachment Handler [\#326](https://github.com/networknt/light-4j/issues/326)
- refactor the dump handler to support response body logging [\#23](https://github.com/networknt/light-4j/issues/23)

**Closed issues:**

- ignore the random number test in balance [\#338](https://github.com/networknt/light-4j/issues/338)
- add a new services method to the Cluster [\#336](https://github.com/networknt/light-4j/issues/336)
- make round robin load balance start with random number [\#335](https://github.com/networknt/light-4j/issues/335)
- A bug that shows Unknown protocol light in service discovery [\#334](https://github.com/networknt/light-4j/issues/334)
- In consul client the hard coded wait value changed through configuration.  [\#332](https://github.com/networknt/light-4j/issues/332)
- add a new method to get local IP for interactive queries [\#331](https://github.com/networknt/light-4j/issues/331)

**Merged pull requests:**

- Issue \#332, \(API 6\) in consul client the wait hard coded value changed through configuration  [\#333](https://github.com/networknt/light-4j/pull/333) ([sreenicibc](https://github.com/sreenicibc))
- \#326 created a form handler [\#329](https://github.com/networknt/light-4j/pull/329) ([jiachen1120](https://github.com/jiachen1120))
- Refactor the dump handler to support response body logging [\#328](https://github.com/networknt/light-4j/pull/328) ([BalloonWen](https://github.com/BalloonWen))

## [1.5.23](https://github.com/networknt/light-4j/tree/1.5.23) (2018-12-01)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.22...1.5.23)

**Closed issues:**

- light-4j benchmark [\#315](https://github.com/networknt/light-4j/issues/315)
- Need to allow server to be embedded [\#312](https://github.com/networknt/light-4j/issues/312)

## [1.5.22](https://github.com/networknt/light-4j/tree/1.5.22) (2018-11-10)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.21...1.5.22)

**Fixed bugs:**

- handle the situation that the alias is not server in the server.keystore [\#317](https://github.com/networknt/light-4j/issues/317)

**Closed issues:**

- add monad-result module to wrap success T and failure Status [\#325](https://github.com/networknt/light-4j/issues/325)
- add successful status code to status.yml [\#324](https://github.com/networknt/light-4j/issues/324)
- gzip and deflate encoding and decoding support in middleware handlers [\#323](https://github.com/networknt/light-4j/issues/323)
- add static CONFIG\_NAME to serverConfig [\#320](https://github.com/networknt/light-4j/issues/320)
- add another default method in LightHttpHandler to bubble up the status [\#319](https://github.com/networknt/light-4j/issues/319)
- support default path in handler.yml for single page application [\#316](https://github.com/networknt/light-4j/issues/316)
- update status.yml ERR10016 to have only one parameter [\#314](https://github.com/networknt/light-4j/issues/314)

**Merged pull requests:**

- fixes \#316 update resource and handler to support SPA from handler.yml [\#318](https://github.com/networknt/light-4j/pull/318) ([stevehu](https://github.com/stevehu))
- Refactored code from main\(\) to init\(\) so server can be embedded. [\#311](https://github.com/networknt/light-4j/pull/311) ([farrukhnajmi](https://github.com/farrukhnajmi))

## [1.5.21](https://github.com/networknt/light-4j/tree/1.5.21) (2018-10-26)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.20...1.5.21)

**Implemented enhancements:**

- Collect environment element in the Metrics Handler [\#310](https://github.com/networknt/light-4j/issues/310)
- Enhance logging in the AuditHandler [\#295](https://github.com/networknt/light-4j/issues/295)
- Environment config in client only applications [\#272](https://github.com/networknt/light-4j/issues/272)

**Fixed bugs:**

- Set correct status code if Method or URI from request could not be resolved in the handler chain [\#308](https://github.com/networknt/light-4j/issues/308)

## [1.5.20](https://github.com/networknt/light-4j/tree/1.5.20) (2018-10-05)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.19...1.5.20)

**Fixed bugs:**

- MiddlewareHandler.register is called for each endpoint [\#305](https://github.com/networknt/light-4j/issues/305)

**Closed issues:**

- Handler needs to properly escape characters in the Status description [\#306](https://github.com/networknt/light-4j/issues/306)
- provide default security.yml and move providerId to jwt.yml [\#304](https://github.com/networknt/light-4j/issues/304)
- Indicate that the BufferSize is too small in client.yml if Body cannot be parsed [\#302](https://github.com/networknt/light-4j/issues/302)
- Intermittent issues with Consul API discovery [\#301](https://github.com/networknt/light-4j/issues/301)
- make bufferSize configurable for default buffer pool in Http2Client [\#299](https://github.com/networknt/light-4j/issues/299)
- Add more debug info during startup for the Kubernetes status.hostIP [\#297](https://github.com/networknt/light-4j/issues/297)
- Update the config module output to error only when config file not found [\#294](https://github.com/networknt/light-4j/issues/294)
- Update OAuthHelper to include new method to support SAML grant type flow [\#290](https://github.com/networknt/light-4j/issues/290)
- server does not create zip file from config server correctly [\#157](https://github.com/networknt/light-4j/issues/157)

## [1.5.19](https://github.com/networknt/light-4j/tree/1.5.19) (2018-09-22)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.18...1.5.19)

**Fixed bugs:**

- Consul heartbeat stream limit being exceeded [\#279](https://github.com/networknt/light-4j/issues/279)

**Closed issues:**

- move light-tokenization status codes to status.ym in light-4j [\#289](https://github.com/networknt/light-4j/issues/289)
- remove unused status code from status.yml [\#288](https://github.com/networknt/light-4j/issues/288)
- add status code for user-management in light-portal [\#287](https://github.com/networknt/light-4j/issues/287)
- fix a typo in HashUtil [\#286](https://github.com/networknt/light-4j/issues/286)
- BodyHandler Middleware to support configurable Content-Type [\#285](https://github.com/networknt/light-4j/issues/285)
- add pattern matching to differentiate email and userId in StringUtils [\#283](https://github.com/networknt/light-4j/issues/283)
- update EmailSender to trust the host from the email.yml [\#278](https://github.com/networknt/light-4j/issues/278)
- change email module name to email-sender [\#277](https://github.com/networknt/light-4j/issues/277)
- create http-url module for url related utility [\#276](https://github.com/networknt/light-4j/issues/276)
- create a new http-string module that depends on Undertow [\#275](https://github.com/networknt/light-4j/issues/275)
- add replaceOnce to StringUtil in utility module [\#274](https://github.com/networknt/light-4j/issues/274)
- set the right default port number for DirectRegistry [\#273](https://github.com/networknt/light-4j/issues/273)
- add error codes for light-config-server [\#271](https://github.com/networknt/light-4j/issues/271)
- Created by accident [\#270](https://github.com/networknt/light-4j/issues/270)
- rename datasource to data-source [\#269](https://github.com/networknt/light-4j/issues/269)
- rename deref to deref-token [\#268](https://github.com/networknt/light-4j/issues/268)
- rename limit to rate-limit [\#267](https://github.com/networknt/light-4j/issues/267)
- rename basic to basic-auth [\#266](https://github.com/networknt/light-4j/issues/266)
- update deregisterAfter from 90m to 2m [\#264](https://github.com/networknt/light-4j/issues/264)
- OpenAPI and GraphQL ValidatorHandlers conflict on config file names [\#252](https://github.com/networknt/light-4j/issues/252)
- add a handler for IP whitelisting [\#235](https://github.com/networknt/light-4j/issues/235)

**Merged pull requests:**

- Updated oauth helper files to handle SAMLBearer grant type [\#292](https://github.com/networknt/light-4j/pull/292) ([dguncb](https://github.com/dguncb))
- Update Readme [\#284](https://github.com/networknt/light-4j/pull/284) ([anilmuppalla](https://github.com/anilmuppalla))
- Related to \#249, adds an EndpointSource interface for injecting path,… [\#282](https://github.com/networknt/light-4j/pull/282) ([logi](https://github.com/logi))
- fixes \#279 Consul heartbeat stream limit being exceeded [\#281](https://github.com/networknt/light-4j/pull/281) ([stevehu](https://github.com/stevehu))
- enhancement for light-oauth2 provider module [\#265](https://github.com/networknt/light-4j/pull/265) ([stevehu](https://github.com/stevehu))

## [1.5.18](https://github.com/networknt/light-4j/tree/1.5.18) (2018-08-15)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.17...1.5.18)

**Implemented enhancements:**

- Support HandlerProvider definitions in handler.yml configuration definitions [\#258](https://github.com/networknt/light-4j/issues/258)
- When stopping the server, give 30 seconds grace period to let service discovery propagate to all clients [\#20](https://github.com/networknt/light-4j/issues/20)

**Fixed bugs:**

- Fix issue causing path variables not to show up in query params when using new handler config [\#250](https://github.com/networknt/light-4j/issues/250)
- Reproduce issue in handler chaining, ensure that the MiddlewareHandler interface is respected [\#247](https://github.com/networknt/light-4j/issues/247)

**Closed issues:**

- Re-starting Server no longer works [\#263](https://github.com/networknt/light-4j/issues/263)
- health endpoint with serviceId as path parameter [\#262](https://github.com/networknt/light-4j/issues/262)
- Address graceful server shutdown while encountering an exception during start-up [\#261](https://github.com/networknt/light-4j/issues/261)
- add a default consul.yml for consul client [\#260](https://github.com/networknt/light-4j/issues/260)
- flatten the config files into the same directory for k8s [\#257](https://github.com/networknt/light-4j/issues/257)
- add getJwtClaimsWithExpiresIn for digital signing only in light-oauth2 token service [\#256](https://github.com/networknt/light-4j/issues/256)
- remove description in the CorrelationHandler config file [\#255](https://github.com/networknt/light-4j/issues/255)
- enable http2 for consul client when TLS is enabled [\#246](https://github.com/networknt/light-4j/issues/246)
- catastrophic setExchangeStatus calls without args [\#244](https://github.com/networknt/light-4j/issues/244)
- upgrade to undertow 2.0.11.Final [\#243](https://github.com/networknt/light-4j/issues/243)
- several enhancements for Consul registration [\#242](https://github.com/networknt/light-4j/issues/242)
- networknt page is not working [\#238](https://github.com/networknt/light-4j/issues/238)
- extend Http2Client to OAuth 2.0 provider communication to support arbitrary number of parameters [\#181](https://github.com/networknt/light-4j/issues/181)

**Merged pull requests:**

- Adding HandlerProvider support to list of handlers. [\#259](https://github.com/networknt/light-4j/pull/259) ([NicholasAzar](https://github.com/NicholasAzar))
- Do not require ignored config [\#254](https://github.com/networknt/light-4j/pull/254) ([logi](https://github.com/logi))
-  Descriptive Exception on unknown chain or handler in handler.yml [\#253](https://github.com/networknt/light-4j/pull/253) ([logi](https://github.com/logi))
- Fix issue causing query params to be missing from exchange. [\#251](https://github.com/networknt/light-4j/pull/251) ([NicholasAzar](https://github.com/NicholasAzar))
- Safer set exchange status [\#245](https://github.com/networknt/light-4j/pull/245) ([logi](https://github.com/logi))

## [1.5.17](https://github.com/networknt/light-4j/tree/1.5.17) (2018-07-15)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.16...1.5.17)

**Closed issues:**

- update readme.md to fix the document links [\#239](https://github.com/networknt/light-4j/issues/239)

## [1.5.16](https://github.com/networknt/light-4j/tree/1.5.16) (2018-07-05)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.15...1.5.16)

**Implemented enhancements:**

- Add "severity" to Status [\#232](https://github.com/networknt/light-4j/issues/232)
- Extending handler chain configuration functionality [\#222](https://github.com/networknt/light-4j/issues/222)

**Closed issues:**

- move HandlerProvider interface to handler package from server [\#236](https://github.com/networknt/light-4j/issues/236)
- separate de-reference middleware handler to its own module [\#233](https://github.com/networknt/light-4j/issues/233)
- add error status for token dereference in light-oauth2 token service [\#230](https://github.com/networknt/light-4j/issues/230)
- add an error status code for light-oauth2 client registration [\#229](https://github.com/networknt/light-4j/issues/229)
- log the class, method, file and line number for status [\#228](https://github.com/networknt/light-4j/issues/228)
- move the basic authentication middleware handler from light-rest-4j [\#226](https://github.com/networknt/light-4j/issues/226)
- enhance client module to add de-reference token in OauthHelper [\#225](https://github.com/networknt/light-4j/issues/225)
- Add a middleware handler to de-reference opaque access token to JWT [\#224](https://github.com/networknt/light-4j/issues/224)
- log error if config file cannot be found in all possible locations [\#223](https://github.com/networknt/light-4j/issues/223)

**Merged pull requests:**

- Feat/path middleware handler [\#227](https://github.com/networknt/light-4j/pull/227) ([NicholasAzar](https://github.com/NicholasAzar))

## [1.5.15](https://github.com/networknt/light-4j/tree/1.5.15) (2018-06-18)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.14...1.5.15)

**Implemented enhancements:**

- Dynamic server  listener port configuration [\#210](https://github.com/networknt/light-4j/issues/210)
- Unexpected behavior when Status is created for non-existent status code [\#169](https://github.com/networknt/light-4j/issues/169)

**Closed issues:**

- add datasource module for most popular relational databases [\#220](https://github.com/networknt/light-4j/issues/220)
- create LightHttpHandler with default method to handle the error status [\#217](https://github.com/networknt/light-4j/issues/217)
- A default content-type is not set [\#216](https://github.com/networknt/light-4j/issues/216)
- add a new status code in status.yml for authenticate class not found [\#215](https://github.com/networknt/light-4j/issues/215)
- Add a Build Number to the server.yml [\#214](https://github.com/networknt/light-4j/issues/214)
- add constants to utility Constants for light-router [\#212](https://github.com/networknt/light-4j/issues/212)
- OAuthHelper needs to handle the error status from OAuth 2.0 provider [\#202](https://github.com/networknt/light-4j/issues/202)

**Merged pull requests:**

- Adding InputStream to String conversion utility [\#219](https://github.com/networknt/light-4j/pull/219) ([NicholasAzar](https://github.com/NicholasAzar))
- Fix216 [\#218](https://github.com/networknt/light-4j/pull/218) ([rpinaa](https://github.com/rpinaa))
- testing addition of graceful shutdown handler [\#213](https://github.com/networknt/light-4j/pull/213) ([NicholasAzar](https://github.com/NicholasAzar))

## [1.5.14](https://github.com/networknt/light-4j/tree/1.5.14) (2018-05-19)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.13...1.5.14)

**Closed issues:**

- update client.yml and secret.yml in other modules [\#211](https://github.com/networknt/light-4j/issues/211)
- Rename resources module to resource for consistency [\#209](https://github.com/networknt/light-4j/issues/209)
- update client module to support refresh token flow for light-spa-4j [\#208](https://github.com/networknt/light-4j/issues/208)
- update verifyJwt with ignoreExpiry flag [\#207](https://github.com/networknt/light-4j/issues/207)
- add default method setExchangeStatus into MiddlewareHandler [\#206](https://github.com/networknt/light-4j/issues/206)
- move auth and csrf to light-spa-4j repository [\#205](https://github.com/networknt/light-4j/issues/205)
- add error code for light-spa-4j stateless-token handler [\#204](https://github.com/networknt/light-4j/issues/204)
- switch StatelessAuthHandler to middleware handler [\#203](https://github.com/networknt/light-4j/issues/203)
- add stateless auth and csrf handlers for SPA application [\#201](https://github.com/networknt/light-4j/issues/201)
- pass csrf token in oauth token request [\#200](https://github.com/networknt/light-4j/issues/200)
- add status code for light-proxy tableau authentication handler [\#199](https://github.com/networknt/light-4j/issues/199)
- move password in jwt.yml to secret.yml [\#198](https://github.com/networknt/light-4j/issues/198)
- share the connections to consul in ConsulClientImpl [\#196](https://github.com/networknt/light-4j/issues/196)

**Merged pull requests:**

- Feature/resource providers [\#197](https://github.com/networknt/light-4j/pull/197) ([stevehu](https://github.com/stevehu))

## [1.5.13](https://github.com/networknt/light-4j/tree/1.5.13) (2018-04-20)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.12...1.5.13)

**Implemented enhancements:**

- create a middleware handler for Prometheus [\#150](https://github.com/networknt/light-4j/issues/150)
- Sanitizer mangles content [\#51](https://github.com/networknt/light-4j/issues/51)

**Closed issues:**

- light-codegen command line error handling [\#195](https://github.com/networknt/light-4j/issues/195)
- upgrade jackson library to 2.9.4 [\#187](https://github.com/networknt/light-4j/issues/187)
- put client credentials token into authorization header instead of X-Scope-Token header [\#185](https://github.com/networknt/light-4j/issues/185)
- name convention between metrics and prometheus [\#165](https://github.com/networknt/light-4j/issues/165)

## [1.5.12](https://github.com/networknt/light-4j/tree/1.5.12) (2018-04-08)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.11...1.5.12)

**Closed issues:**

- upgrade to prometheus 0.3.0 [\#192](https://github.com/networknt/light-4j/issues/192)
- rollback zkclient to 0.3 [\#191](https://github.com/networknt/light-4j/issues/191)
- remove consul client and update version for zkclient and curator [\#190](https://github.com/networknt/light-4j/issues/190)
- remove antlr4 from dependencies [\#189](https://github.com/networknt/light-4j/issues/189)
- upgrade jackson 2.9.5 remove swagger from light-4j dependencies [\#188](https://github.com/networknt/light-4j/issues/188)
- add two more test cases for Http2Client [\#186](https://github.com/networknt/light-4j/issues/186)

## [1.5.11](https://github.com/networknt/light-4j/tree/1.5.11) (2018-03-31)
[Full Changelog](https://github.com/networknt/light-4j/compare/1.5.10...1.5.11)

**Fixed bugs:**

- JWT signing private key cannot be externalized [\#178](https://github.com/networknt/light-4j/issues/178)
- scope encoding changes space to plus sign in OAuthHelper [\#172](https://github.com/networknt/light-4j/issues/172)

**Closed issues:**

- add ERR11300 FAIL\_TO\_GET\_TABLEAU\_TOKEN error code in status.yml [\#184](https://github.com/networknt/light-4j/issues/184)
- check if trace is enabled in Http2Client logger [\#183](https://github.com/networknt/light-4j/issues/183)
- add isBlank to StringUtil in utility class [\#180](https://github.com/networknt/light-4j/issues/180)
- split JwtHelper to JwtHelper for token verification and JwtIssuer to issue token [\#179](https://github.com/networknt/light-4j/issues/179)
- remove Apache commons-lang dependency from mask [\#177](https://github.com/networknt/light-4j/issues/177)
- remove commons-io dependencies [\#176](https://github.com/networknt/light-4j/issues/176)
- refactor CorsHttpHandlerTest to use Http2Client [\#175](https://github.com/networknt/light-4j/issues/175)
- add status code ERR11202 for hybrid-4j get request [\#173](https://github.com/networknt/light-4j/issues/173)
- remove unused import in DecryptUtil [\#171](https://github.com/networknt/light-4j/issues/171)

**Merged pull requests:**

- feat\(util\): Add toByteBuffer overload util method to convert files. [\#174](https://github.com/networknt/light-4j/pull/174) ([NicholasAzar](https://github.com/NicholasAzar))

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

- Upgraded Undertow, Added Utility Interfaces for Header, path, query extraction [\#111](https://github.com/networknt/light-4j/pull/111) ([sachin-walia](https://github.com/sachin-walia))
- Resolved \#1 Upgrading libraries and other changes [\#107](https://github.com/networknt/light-4j/pull/107) ([sachin-walia](https://github.com/sachin-walia))

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
