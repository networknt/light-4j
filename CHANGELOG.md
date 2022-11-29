# Change Log

## [2.1.3](https://github.com/networknt/light-4j/tree/2.1.3) (2022-11-10)


**Merged pull requests:**


- fixes \#1466 recreate RateLimiter object after the config reload [\#1467](https://github.com/networknt/light-4j/pull/1467) ([stevehu](https://github.com/stevehu))
- fixes \#1464 update path handlers to allow the mapping to be empty [\#1465](https://github.com/networknt/light-4j/pull/1465) ([stevehu](https://github.com/stevehu))
- fixes \#1460 do not skip PathPrefixServiceHandler if server_url is in … [\#1463](https://github.com/networknt/light-4j/pull/1463) ([stevehu](https://github.com/stevehu))
- fixes \#1461 mask bootstrapStorePass in the server config during regis… [\#1462](https://github.com/networknt/light-4j/pull/1462) ([stevehu](https://github.com/stevehu))
- fixes \#1458 implement the password grant_type for salesforce handler [\#1459](https://github.com/networknt/light-4j/pull/1459) ([stevehu](https://github.com/stevehu))
- fixes \#1454 standardize the built-in config files for some modules [\#1455](https://github.com/networknt/light-4j/pull/1455) ([stevehu](https://github.com/stevehu))
- fixes \#1451 add api-key module and dummy OAuth server [\#1452](https://github.com/networknt/light-4j/pull/1452) ([stevehu](https://github.com/stevehu))
- fixes \#1449 udpate the BasicAuthHandler to make the config an instanc… [\#1450](https://github.com/networknt/light-4j/pull/1450) ([stevehu](https://github.com/stevehu))
- Added shutdownApp method [\#1447](https://github.com/networknt/light-4j/pull/1447) ([fortunadoralph](https://github.com/fortunadoralph))
- fixes \#1445 support multiple rules in the response transformer interc… [\#1446](https://github.com/networknt/light-4j/pull/1446) ([stevehu](https://github.com/stevehu))
- Fixed Chunked Encoding When writing transformed Payload to SinkConduit (+ some refactor) [\#1444](https://github.com/networknt/light-4j/pull/1444) ([KalevGonvick](https://github.com/KalevGonvick))
- fixes \#1442 resolve NPE in TokenHandler is appliedPathPrefixes missin… [\#1443](https://github.com/networknt/light-4j/pull/1443) ([stevehu](https://github.com/stevehu))
- fixes \#1439 request and response transform interceptors though NPE if… [\#1440](https://github.com/networknt/light-4j/pull/1440) ([stevehu](https://github.com/stevehu))
- ResponseBodyInterceptor Update [\#1437](https://github.com/networknt/light-4j/pull/1437) ([KalevGonvick](https://github.com/KalevGonvick))
- fixes \#1435 checked content encoding in the response interceptor inje… [\#1436](https://github.com/networknt/light-4j/pull/1436) ([stevehu](https://github.com/stevehu))
- fixes \#1433 update the ModifiableContentSinkConduit to add some trace… [\#1434](https://github.com/networknt/light-4j/pull/1434) ([stevehu](https://github.com/stevehu))
- fixes \#1431 add debug and trace to the ProxyHandler to confirm retry [\#1432](https://github.com/networknt/light-4j/pull/1432) ([stevehu](https://github.com/stevehu))
- fixes \#1429 check the response headers to identify if the response co… [\#1430](https://github.com/networknt/light-4j/pull/1430) ([stevehu](https://github.com/stevehu))
- fixes \#1426 call the interceptors directly if there is no request body [\#1427](https://github.com/networknt/light-4j/pull/1427) ([stevehu](https://github.com/stevehu))
- fixes \#1424 add requestHeaders and responseHeaders to the request res… [\#1425](https://github.com/networknt/light-4j/pull/1425) ([stevehu](https://github.com/stevehu))
## [2.1.2](https://github.com/networknt/light-4j/tree/2.1.2) (2022-10-22)


**Merged pull requests:**


- Fix flaky test in URLNormalizerTest.java [\#1419](https://github.com/networknt/light-4j/pull/1419) ([yannizhou05](https://github.com/yannizhou05))
- fixes \#1418 enhance the token handler to add applied path prefixes [\#1422](https://github.com/networknt/light-4j/pull/1422) ([stevehu](https://github.com/stevehu))
- fixes \#1420 check the appliedPathPrefixes for the RequestTransformInt… [\#1421](https://github.com/networknt/light-4j/pull/1421) ([stevehu](https://github.com/stevehu))
- fixes \#1415 add a new external handler for conquest planning API access [\#1416](https://github.com/networknt/light-4j/pull/1416) ([stevehu](https://github.com/stevehu))
- Updated ModifiableSinkConduit + RequestInterceptInjectionHandler [\#1414](https://github.com/networknt/light-4j/pull/1414) ([KalevGonvick](https://github.com/KalevGonvick))
- fixes \#1412 move the rewrite rules to inside the match condition for … [\#1413](https://github.com/networknt/light-4j/pull/1413) ([stevehu](https://github.com/stevehu))
- fixes \#1407 Add a new config property to control the size of the requ… [\#1410](https://github.com/networknt/light-4j/pull/1410) ([stevehu](https://github.com/stevehu))
- Update RequestInterceptorInjectionHandler.java [\#1409](https://github.com/networknt/light-4j/pull/1409) ([KalevGonvick](https://github.com/KalevGonvick))
- fixes \#1406 adding config reload for router.yml and others used by th… [\#1408](https://github.com/networknt/light-4j/pull/1408) ([stevehu](https://github.com/stevehu))
- fixes \#1402 add skip list for the security.yml to allow some prefix t… [\#1404](https://github.com/networknt/light-4j/pull/1404) ([stevehu](https://github.com/stevehu))
- fixes \#1401 add url rewrite rules for the salesforce handler for exte… [\#1403](https://github.com/networknt/light-4j/pull/1403) ([stevehu](https://github.com/stevehu))
- Issue1399 [\#1400](https://github.com/networknt/light-4j/pull/1400) ([stevehu](https://github.com/stevehu))
- fixes \#1396 remove the stream body get as start blocking is remove [\#1397](https://github.com/networknt/light-4j/pull/1397) ([stevehu](https://github.com/stevehu))
- fixes \#1394 add properties to allow connect and host update in header [\#1395](https://github.com/networknt/light-4j/pull/1395) ([stevehu](https://github.com/stevehu))
- fixes \#1392 remove blocking as it causes the ModifiableContentSinkCon… [\#1393](https://github.com/networknt/light-4j/pull/1393) ([stevehu](https://github.com/stevehu))
- fixes \#1390 update response headers for external service handler to r… [\#1391](https://github.com/networknt/light-4j/pull/1391) ([stevehu](https://github.com/stevehu))
- fix for RequestTransformer plus debug logging for ResponseTransformer [\#1389](https://github.com/networknt/light-4j/pull/1389) ([DiogoFKT](https://github.com/DiogoFKT))
- Update RequestInterceptorInjectionHandler.java [\#1387](https://github.com/networknt/light-4j/pull/1387) ([KalevGonvick](https://github.com/KalevGonvick))
- fixes \#1384 fix a bug in the AuditConfig for the enabled flag [\#1385](https://github.com/networknt/light-4j/pull/1385) ([stevehu](https://github.com/stevehu))
- fixes \#1382 stop calling next middleware handler in request andd resp… [\#1383](https://github.com/networknt/light-4j/pull/1383) ([stevehu](https://github.com/stevehu))
- fixes \#1380 external salesforce and mras reponse header missing [\#1381](https://github.com/networknt/light-4j/pull/1381) ([stevehu](https://github.com/stevehu))
- fixes \#1378 add a config property to pre-resolve the host in egress-r… [\#1379](https://github.com/networknt/light-4j/pull/1379) ([stevehu](https://github.com/stevehu))
- fixes \#1376 allow the empty body for RequestBodyInterceptor and all e… [\#1377](https://github.com/networknt/light-4j/pull/1377) ([stevehu](https://github.com/stevehu))
- Feature/body handler trace enhancement [\#1374](https://github.com/networknt/light-4j/pull/1374) ([KalevGonvick](https://github.com/KalevGonvick))
- fixes \#1372 ExternalService handler cannot handle empty body [\#1375](https://github.com/networknt/light-4j/pull/1375) ([stevehu](https://github.com/stevehu))
- fixes \#1371 add url rewrite to the external service handler in egress… [\#1373](https://github.com/networknt/light-4j/pull/1373) ([stevehu](https://github.com/stevehu))
- fixes \#1369 update header handler to support header manipulation per … [\#1370](https://github.com/networknt/light-4j/pull/1370) ([stevehu](https://github.com/stevehu))
- Changes made to the code as required. Mentioned in issue \#1274 & \#1287 [\#1357](https://github.com/networknt/light-4j/pull/1357) ([AkashWorkGit](https://github.com/AkashWorkGit))
- fixes \#1367 handle the null content type for the mras request [\#1368](https://github.com/networknt/light-4j/pull/1368) ([stevehu](https://github.com/stevehu))
- fixes \#1363 add url rewrite rules in the mras handler [\#1366](https://github.com/networknt/light-4j/pull/1366) ([stevehu](https://github.com/stevehu))
- fixes \#1364 update the logic to copy the same hosts configuration to … [\#1365](https://github.com/networknt/light-4j/pull/1365) ([stevehu](https://github.com/stevehu))
- Issue1361 [\#1362](https://github.com/networknt/light-4j/pull/1362) ([stevehu](https://github.com/stevehu))
- fixes \#1359 update MRAS handler to load the certificate from the keys… [\#1360](https://github.com/networknt/light-4j/pull/1360) ([stevehu](https://github.com/stevehu))
- h2c config option [\#1358](https://github.com/networknt/light-4j/pull/1358) ([KalevGonvick](https://github.com/KalevGonvick))
- fixes \#1355 Donot run response body interceptor if the response is st… [\#1356](https://github.com/networknt/light-4j/pull/1356) ([stevehu](https://github.com/stevehu))
- fixes \#1352 disable all test cases for the request body interceptor [\#1353](https://github.com/networknt/light-4j/pull/1353) ([stevehu](https://github.com/stevehu))
- fixes \#1350 do not call the next handler in the chain from request bo… [\#1351](https://github.com/networknt/light-4j/pull/1351) ([stevehu](https://github.com/stevehu))
- fixes \#1348 A typo in the proxy.yml in ingress-proxy resources/config [\#1349](https://github.com/networknt/light-4j/pull/1349) ([stevehu](https://github.com/stevehu))
- fixes \#1346 fix typos in the Salesforce and MRAS handlers to set the … [\#1347](https://github.com/networknt/light-4j/pull/1347) ([stevehu](https://github.com/stevehu))
- fixes \#1344 update salesforce handler to support multiple APIs with d… [\#1345](https://github.com/networknt/light-4j/pull/1345) ([stevehu](https://github.com/stevehu))
- fix typo for ExternalService response headers [\#1343](https://github.com/networknt/light-4j/pull/1343) ([DiogoFKT](https://github.com/DiogoFKT))
- Fix for 1339 [\#1340](https://github.com/networknt/light-4j/pull/1340) ([KalevGonvick](https://github.com/KalevGonvick))
- fixes \#1337 disable several test cases in client module that only wor… [\#1338](https://github.com/networknt/light-4j/pull/1338) ([stevehu](https://github.com/stevehu))
- fixes \#1335 check the request path with the key set in MRAS and Sales… [\#1336](https://github.com/networknt/light-4j/pull/1336) ([stevehu](https://github.com/stevehu))
- fixes \#1332 refactor salesforce handle to support multiple configurat… [\#1333](https://github.com/networknt/light-4j/pull/1333) ([stevehu](https://github.com/stevehu))
- fixes \#1330 update MRAS config to add resource to the Microsoft token… [\#1331](https://github.com/networknt/light-4j/pull/1331) ([stevehu](https://github.com/stevehu))
- fixes \#1328 Handle non-JSON request body in the AuditHandler [\#1329](https://github.com/networknt/light-4j/pull/1329) ([stevehu](https://github.com/stevehu))
- fixes \#1326 refactor the MRAS handler to resolve multiple use cases w… [\#1327](https://github.com/networknt/light-4j/pull/1327) ([stevehu](https://github.com/stevehu))
- fixes \#1324 support xml request body parsing in the RequestBodyInterc… [\#1325](https://github.com/networknt/light-4j/pull/1325) ([stevehu](https://github.com/stevehu))
- Bump postgresql from 42.3.3 to 42.4.1 [\#1321](https://github.com/networknt/light-4j/pull/1321) ([dependabot](https://github.com/apps/dependabot))
- fixes \#1319 remove the status from the audit list [\#1320](https://github.com/networknt/light-4j/pull/1320) ([stevehu](https://github.com/stevehu))
- fixes \#1317 convert the audit handler to interceptor for logging requ… [\#1318](https://github.com/networknt/light-4j/pull/1318) ([stevehu](https://github.com/stevehu))
- fixes \#1314 upgrade yaml-rule to 1.0.1 in pom.xml [\#1315](https://github.com/networknt/light-4j/pull/1315) ([stevehu](https://github.com/stevehu))
- Issue1308 [\#1312](https://github.com/networknt/light-4j/pull/1312) ([stevehu](https://github.com/stevehu))
- fixes \#1302 Do an iteration to get the serviceId from the request path [\#1303](https://github.com/networknt/light-4j/pull/1303) ([stevehu](https://github.com/stevehu))
- fixes \#1300 update ServiceConfig to allow the singletons to be null [\#1301](https://github.com/networknt/light-4j/pull/1301) ([stevehu](https://github.com/stevehu))
- fixes \#1298 cache the jwk with all kids in the jwk result [\#1299](https://github.com/networknt/light-4j/pull/1299) ([stevehu](https://github.com/stevehu))
- fixes \#1294 update BasicAuthConfig to support JSON string for the users [\#1295](https://github.com/networknt/light-4j/pull/1295) ([stevehu](https://github.com/stevehu))
- fixes \#1292 upload ServiceConfig to support load singletons as JSON s… [\#1293](https://github.com/networknt/light-4j/pull/1293) ([stevehu](https://github.com/stevehu))
- fixes \#1290 update GatewayRouterHandler to add the caller_id to the r… [\#1291](https://github.com/networknt/light-4j/pull/1291) ([stevehu](https://github.com/stevehu))
- fixes \#1288 change the ProxyConfig to remove httpsEnabled [\#1289](https://github.com/networknt/light-4j/pull/1289) ([stevehu](https://github.com/stevehu))
- fixes \#1285 add hosts and path for debugging in proxy handlers [\#1286](https://github.com/networknt/light-4j/pull/1286) ([stevehu](https://github.com/stevehu))
- fixes \#1283 update gateway service dict handler to use HandlerUtils.t… [\#1284](https://github.com/networknt/light-4j/pull/1284) ([stevehu](https://github.com/stevehu))
- fixes \#1281 refactor the ServiceDictConfig to use internal format [\#1282](https://github.com/networknt/light-4j/pull/1282) ([stevehu](https://github.com/stevehu))
- fixes \#1279 update path service handlers with new config class to sup… [\#1280](https://github.com/networknt/light-4j/pull/1280) ([stevehu](https://github.com/stevehu))
- fixes \#1277 update TokenKeyRequest to make the clientId and clientSec… [\#1278](https://github.com/networknt/light-4j/pull/1278) ([stevehu](https://github.com/stevehu))
- fixes \#1275 update AuditConfig methods to public so that it can be ac… [\#1276](https://github.com/networknt/light-4j/pull/1276) ([stevehu](https://github.com/stevehu))
- fixes \#1272 update security.yml to use JsonWebKeySet for keyResolver [\#1273](https://github.com/networknt/light-4j/pull/1273) ([stevehu](https://github.com/stevehu))
- fixes \#1270 add providerId to the security.yml for oauth key service [\#1271](https://github.com/networknt/light-4j/pull/1271) ([stevehu](https://github.com/stevehu))
- fixes \#1268 gracefully handle the security config missing in the Secu… [\#1269](https://github.com/networknt/light-4j/pull/1269) ([stevehu](https://github.com/stevehu))
- fixes \#1266 update the security.yml with SecurityConfig class to supp… [\#1267](https://github.com/networknt/light-4j/pull/1267) ([stevehu](https://github.com/stevehu))
- fixes \#1264 upgrade json-path and remove the exclusion in the pom.xml [\#1265](https://github.com/networknt/light-4j/pull/1265) ([stevehu](https://github.com/stevehu))
- fixes \#1262 externalize module configurations for default [\#1263](https://github.com/networknt/light-4j/pull/1263) ([stevehu](https://github.com/stevehu))
- fixes \#1260 externalize the audit.yml andupdate the AuditConfig to su… [\#1261](https://github.com/networknt/light-4j/pull/1261) ([stevehu](https://github.com/stevehu))
- fixes \#1258 add logging statements to the SingletonServiceFactory [\#1259](https://github.com/networknt/light-4j/pull/1259) ([stevehu](https://github.com/stevehu))
- fixes \#1256 add default service.yml with template in service module [\#1257](https://github.com/networknt/light-4j/pull/1257) ([stevehu](https://github.com/stevehu))
- fixes \#1254 remove the full jwt from the logging for security reason [\#1255](https://github.com/networknt/light-4j/pull/1255) ([stevehu](https://github.com/stevehu))
- fixes \#1252 resolve a class cast exception in TokenHandler [\#1253](https://github.com/networknt/light-4j/pull/1253) ([stevehu](https://github.com/stevehu))
- fixes \#1250 change the passwords and secrets in client.yml and server… [\#1251](https://github.com/networknt/light-4j/pull/1251) ([stevehu](https://github.com/stevehu))
- fixes \#1248 update the TokenHandler to support multiple OAuth 2.0 pro… [\#1249](https://github.com/networknt/light-4j/pull/1249) ([stevehu](https://github.com/stevehu))
- fixes \#1246 update client module to copy the config map for registry [\#1247](https://github.com/networknt/light-4j/pull/1247) ([stevehu](https://github.com/stevehu))
- fixes \#1244 add trace info in ExternalServiceHandler for debugging [\#1245](https://github.com/networknt/light-4j/pull/1245) ([stevehu](https://github.com/stevehu))
- fixes \#1242 externalize scope with a list of strings in client.yml fo… [\#1243](https://github.com/networknt/light-4j/pull/1243) ([stevehu](https://github.com/stevehu))
- fixes \#1240 add ssl context to the connection for the external servic… [\#1241](https://github.com/networknt/light-4j/pull/1241) ([stevehu](https://github.com/stevehu))
- fixes \#1238 add values.yml with serviceId and rename the test values.yml [\#1239](https://github.com/networknt/light-4j/pull/1239) ([stevehu](https://github.com/stevehu))
- Added shutdown handler [\#1237](https://github.com/networknt/light-4j/pull/1237) ([mayurikasaxena](https://github.com/mayurikasaxena))
- fixes \#1235 add MRAS handler for external service authentication and … [\#1236](https://github.com/networknt/light-4j/pull/1236) ([stevehu](https://github.com/stevehu))
- Issue1233 [\#1234](https://github.com/networknt/light-4j/pull/1234) ([stevehu](https://github.com/stevehu))
- fixes \#1231 add a generic handler to access external services via pro… [\#1232](https://github.com/networknt/light-4j/pull/1232) ([stevehu](https://github.com/stevehu))
- fixes \#1229 update salesforce handler and config [\#1230](https://github.com/networknt/light-4j/pull/1230) ([stevehu](https://github.com/stevehu))
- fixes \#1227 add some debug statements to the JWT access in client and… [\#1228](https://github.com/networknt/light-4j/pull/1228) ([stevehu](https://github.com/stevehu))
- add empty checking for scopes [\#1226](https://github.com/networknt/light-4j/pull/1226) ([fortunadoralph](https://github.com/fortunadoralph))
- fixes \#1224 Add a Salesforce handler to authentication and invocation [\#1225](https://github.com/networknt/light-4j/pull/1225) ([stevehu](https://github.com/stevehu))
## [2.1.1](https://github.com/networknt/light-4j/tree/2.1.1) (2022-04-26)


**Merged pull requests:**


- fix for NPE if input is null for Mask methods (issue 1208) [\#1222](https://github.com/networknt/light-4j/pull/1222) ([miklish](https://github.com/miklish))
- fixes \#1220 update the rate-limit config to ensure backward compatibi… [\#1221](https://github.com/networknt/light-4j/pull/1221) ([stevehu](https://github.com/stevehu))
- fixes \#1216 add query parameter and header rewrite in the ProxyHandler [\#1217](https://github.com/networknt/light-4j/pull/1217) ([stevehu](https://github.com/stevehu))
- fixes \#1218 handle the case that clientId and userId resolver failed … [\#1219](https://github.com/networknt/light-4j/pull/1219) ([stevehu](https://github.com/stevehu))
- Issue1211 [\#1212](https://github.com/networknt/light-4j/pull/1212) ([stevehu](https://github.com/stevehu))
- fixes \#1213 move the tableau authentication handler to the light-4j i… [\#1214](https://github.com/networknt/light-4j/pull/1214) ([stevehu](https://github.com/stevehu))
- fixes \#1209 NPE is thrown when the server is selected as key without … [\#1210](https://github.com/networknt/light-4j/pull/1210) ([stevehu](https://github.com/stevehu))
- fixes \#1206 update the default rate limit handle configuration after … [\#1207](https://github.com/networknt/light-4j/pull/1207) ([stevehu](https://github.com/stevehu))
- fixes \#1204 update rate-limit to add an overloaded constructor with c… [\#1205](https://github.com/networknt/light-4j/pull/1205) ([stevehu](https://github.com/stevehu))
- fixes \#1202 remove the 500 sleep and enable multiple requests test [\#1203](https://github.com/networknt/light-4j/pull/1203) ([stevehu](https://github.com/stevehu))
- Rate limit handler fix [\#1201](https://github.com/networknt/light-4j/pull/1201) ([GavinChenYan](https://github.com/GavinChenYan))
- Issue1178 [\#1200](https://github.com/networknt/light-4j/pull/1200) ([stevehu](https://github.com/stevehu))
- fixes \#1198 return an status object for generic exception from the Pr… [\#1199](https://github.com/networknt/light-4j/pull/1199) ([stevehu](https://github.com/stevehu))
- Feature/content length error message [\#1197](https://github.com/networknt/light-4j/pull/1197) ([KalevGonvick](https://github.com/KalevGonvick))
- ProxyBodyHandler Rework [\#1196](https://github.com/networknt/light-4j/pull/1196) ([KalevGonvick](https://github.com/KalevGonvick))
- add DefaultConfigLoaderTest.java [\#1192](https://github.com/networknt/light-4j/pull/1192) ([wswjwjccjlu](https://github.com/wswjwjccjlu))
- fixes \#1191 We have ProxyHandler in both egress-router and ingress-pr… [\#1194](https://github.com/networknt/light-4j/pull/1194) ([stevehu](https://github.com/stevehu))
- Issue1188 [\#1189](https://github.com/networknt/light-4j/pull/1189) ([stevehu](https://github.com/stevehu))
- ProxyBodyHandler rework [\#1187](https://github.com/networknt/light-4j/pull/1187) ([KalevGonvick](https://github.com/KalevGonvick))
- fixes \#1183 add the Transfer-Encoding of http header into the client.yml [\#1185](https://github.com/networknt/light-4j/pull/1185) ([stevehu](https://github.com/stevehu))
- fixes \#1181 Update the config class to output the config file name wh… [\#1182](https://github.com/networknt/light-4j/pull/1182) ([stevehu](https://github.com/stevehu))
- fixes \#1179 remove a trace statement that can cause NPE [\#1180](https://github.com/networknt/light-4j/pull/1180) ([stevehu](https://github.com/stevehu))
- fixes \#1176 add a status code for OBJECT_NOT_UNIQUE [\#1177](https://github.com/networknt/light-4j/pull/1177) ([stevehu](https://github.com/stevehu))
- fixes \#1174 [\#1175](https://github.com/networknt/light-4j/pull/1175) ([GavinChenYan](https://github.com/GavinChenYan))
- fixes \#1172 output the status in log if get service from portal fails [\#1173](https://github.com/networknt/light-4j/pull/1173) ([stevehu](https://github.com/stevehu))
- fixes \#1170 add enabled flag to the rule-loader.yml to bypass the rul… [\#1171](https://github.com/networknt/light-4j/pull/1171) ([stevehu](https://github.com/stevehu))
- Update on config loader for nested values.yml [\#1168](https://github.com/networknt/light-4j/pull/1168) ([wswjwjccjlu](https://github.com/wswjwjccjlu))
- fixes \#1166 Handle the LoadBalancingRouterProxyClient has empty host … [\#1167](https://github.com/networknt/light-4j/pull/1167) ([stevehu](https://github.com/stevehu))
- fixes \#1126 update the config.yml and router.yml with templates [\#1165](https://github.com/networknt/light-4j/pull/1165) ([stevehu](https://github.com/stevehu))
- fixes \#1162 Add a new error code for Startup Hook not loaded correctly [\#1163](https://github.com/networknt/light-4j/pull/1163) ([stevehu](https://github.com/stevehu))
- fixes \#1160 Update a typo in a test case comment [\#1161](https://github.com/networknt/light-4j/pull/1161) ([stevehu](https://github.com/stevehu))
- fixes \#1158 update default client.yml to enable the token serverUrl a… [\#1159](https://github.com/networknt/light-4j/pull/1159) ([stevehu](https://github.com/stevehu))
- fixes \#1156 add more tracing statements in OauthHelper [\#1157](https://github.com/networknt/light-4j/pull/1157) ([stevehu](https://github.com/stevehu))
- fixes \#1154 adding logging statements in AbstractRegistry [\#1155](https://github.com/networknt/light-4j/pull/1155) ([stevehu](https://github.com/stevehu))
-  fix the empty body issue for config reload handler [\#1153](https://github.com/networknt/light-4j/pull/1153) ([GavinChenYan](https://github.com/GavinChenYan))
- fixes \#1151 add a default constructor for ClientCredentialsRequest [\#1152](https://github.com/networknt/light-4j/pull/1152) ([stevehu](https://github.com/stevehu))
- fixes \#1149 make the sanitizer.yml backward compatible [\#1150](https://github.com/networknt/light-4j/pull/1150) ([stevehu](https://github.com/stevehu))
- fixes \#1147 remove the serviceId from the header in the router client [\#1148](https://github.com/networknt/light-4j/pull/1148) ([stevehu](https://github.com/stevehu))
- fixes \#1140 Update client module to verify JWT tokens from many OAuth… [\#1146](https://github.com/networknt/light-4j/pull/1146) ([stevehu](https://github.com/stevehu))
- Issue1139 [\#1145](https://github.com/networknt/light-4j/pull/1145) ([stevehu](https://github.com/stevehu))
- Issue1143 [\#1144](https://github.com/networknt/light-4j/pull/1144) ([GavinChenYan](https://github.com/GavinChenYan))
- fixes \#1141 update logging statements in OauthHelper and ProxyHandler [\#1142](https://github.com/networknt/light-4j/pull/1142) ([stevehu](https://github.com/stevehu))
- fixes \#1137 update the rule-loader startup to avoid loading the same … [\#1138](https://github.com/networknt/light-4j/pull/1138) ([stevehu](https://github.com/stevehu))
- fixes \#1135 add a new status code to indicate the access control rule… [\#1136](https://github.com/networknt/light-4j/pull/1136) ([stevehu](https://github.com/stevehu))
- fixes \#1133 Add method rewrite in the gateway use case to support leg… [\#1134](https://github.com/networknt/light-4j/pull/1134) ([stevehu](https://github.com/stevehu))
- fixes \#1131 update sanitizer handler to support all owasp encoders [\#1132](https://github.com/networknt/light-4j/pull/1132) ([stevehu](https://github.com/stevehu))
- fixes \#1129 update RuleLoaderStartupHook to only get the ruleId and i… [\#1130](https://github.com/networknt/light-4j/pull/1130) ([stevehu](https://github.com/stevehu))
- fixes \#1127 upgrade jaeger-client to 1.8.0 from 1.6.0 to resolve depe… [\#1128](https://github.com/networknt/light-4j/pull/1128) ([stevehu](https://github.com/stevehu))
## [2.1.0](https://github.com/networknt/light-4j/tree/2.1.0) (2022-02-27)


**Merged pull requests:**


- fixes \#1124 enhance the sanitizer to make the configuration separated… [\#1125](https://github.com/networknt/light-4j/pull/1125) ([stevehu](https://github.com/stevehu))
- fixes \#1122 log the stacktrace if a middleware handler is not loaded … [\#1123](https://github.com/networknt/light-4j/pull/1123) ([stevehu](https://github.com/stevehu))
- Issue1120 [\#1121](https://github.com/networknt/light-4j/pull/1121) ([stevehu](https://github.com/stevehu))
- fixes \#1118 allow router to support serviceId from query parameters a… [\#1119](https://github.com/networknt/light-4j/pull/1119) ([stevehu](https://github.com/stevehu))
- fixes \#1116 Update the rate-limit to allow customzied the error code … [\#1117](https://github.com/networknt/light-4j/pull/1117) ([stevehu](https://github.com/stevehu))
- fixes \#1112 add Jdk8Module to the ObjectMappers in config module to h… [\#1113](https://github.com/networknt/light-4j/pull/1113) ([stevehu](https://github.com/stevehu))
- fixes \#1108 update the rule-loader to add another rule action to tran… [\#1109](https://github.com/networknt/light-4j/pull/1109) ([stevehu](https://github.com/stevehu))
- Bump postgresql from 42.2.25 to 42.3.3 [\#1107](https://github.com/networknt/light-4j/pull/1107) ([dependabot](https://github.com/apps/dependabot))
- fixes \#1105 disable a test case in the body handler as it is not stable [\#1106](https://github.com/networknt/light-4j/pull/1106) ([stevehu](https://github.com/stevehu))
- Truncated Exception Fix [\#1104](https://github.com/networknt/light-4j/pull/1104) ([KalevGonvick](https://github.com/KalevGonvick))
- fixes \#1102 update the LoggerGetLogContentHandler to return map and h… [\#1103](https://github.com/networknt/light-4j/pull/1103) ([stevehu](https://github.com/stevehu))
- fixes \#1100 remove a logging statement in the DefaultConfigLoader as … [\#1101](https://github.com/networknt/light-4j/pull/1101) ([stevehu](https://github.com/stevehu))
- fixes \#1097 add isNumeric to StringUtils in the utility [\#1098](https://github.com/networknt/light-4j/pull/1098) ([stevehu](https://github.com/stevehu))
- Bump postgresql from 9.4.1211 to 42.2.25 [\#1095](https://github.com/networknt/light-4j/pull/1095) ([dependabot](https://github.com/apps/dependabot))
- Issue1093 [\#1094](https://github.com/networknt/light-4j/pull/1094) ([stevehu](https://github.com/stevehu))
- fixes \#1091 update the default rate limit concurrent requests to 2 fr… [\#1092](https://github.com/networknt/light-4j/pull/1092) ([stevehu](https://github.com/stevehu))
- fixes \#1089 update audit status key from Status to status [\#1090](https://github.com/networknt/light-4j/pull/1090) ([stevehu](https://github.com/stevehu))
- fixes \#1087 externalize rate-limit, header and whitelist-ip config files [\#1088](https://github.com/networknt/light-4j/pull/1088) ([stevehu](https://github.com/stevehu))
- Bump h2 from 2.0.206 to 2.1.210 [\#1086](https://github.com/networknt/light-4j/pull/1086) ([dependabot](https://github.com/apps/dependabot))
- fixes \#1084 update the DefaultConfigLoader to get the values.yml from… [\#1085](https://github.com/networknt/light-4j/pull/1085) ([stevehu](https://github.com/stevehu))
- Bump httpclient from 4.5.6 to 4.5.13 [\#1077](https://github.com/networknt/light-4j/pull/1077) ([dependabot](https://github.com/apps/dependabot))
- Bump h2 from 1.4.196 to 2.0.206 [\#1083](https://github.com/networknt/light-4j/pull/1083) ([dependabot](https://github.com/apps/dependabot))
- fixes \#1081 update the ClaimsUtil to name the service id claim with s… [\#1082](https://github.com/networknt/light-4j/pull/1082) ([stevehu](https://github.com/stevehu))
- fixes \#1079 add method and path to the method not found error message [\#1080](https://github.com/networknt/light-4j/pull/1080) ([stevehu](https://github.com/stevehu))
- fixes \#1075 Add rule-loader module to support fine-grained access con… [\#1076](https://github.com/networknt/light-4j/pull/1076) ([stevehu](https://github.com/stevehu))
- fixes \#1073 update the sanitizer.yml to externalize properties for va… [\#1074](https://github.com/networknt/light-4j/pull/1074) ([stevehu](https://github.com/stevehu))
- fixes \#1071 externalize jaeger-tracing configuration properties [\#1072](https://github.com/networknt/light-4j/pull/1072) ([stevehu](https://github.com/stevehu))
- fixes \#1069 update server.yml to externalize server.ip [\#1070](https://github.com/networknt/light-4j/pull/1070) ([stevehu](https://github.com/stevehu))
- fixes \#1067 update the SignKeyRequest to get the proxy info from the … [\#1068](https://github.com/networknt/light-4j/pull/1068) ([stevehu](https://github.com/stevehu))
- fixes \#1065 Turn off hostname verification for OAuthHelper based on t… [\#1066](https://github.com/networknt/light-4j/pull/1066) ([stevehu](https://github.com/stevehu))
- change promethus config to be extendable [\#1064](https://github.com/networknt/light-4j/pull/1064) ([GavinChenYan](https://github.com/GavinChenYan))
- fixes \#1061 [\#1062](https://github.com/networknt/light-4j/pull/1062) ([GavinChenYan](https://github.com/GavinChenYan))
- Issue1059 [\#1060](https://github.com/networknt/light-4j/pull/1060) ([stevehu](https://github.com/stevehu))
- fixes \#1057 add ProxyHealthGetHandler in ingress-proxy for the http-s… [\#1058](https://github.com/networknt/light-4j/pull/1058) ([stevehu](https://github.com/stevehu))
- fixes \#1053 update the pom.xml and jaeger-client dependency to avoid … [\#1054](https://github.com/networknt/light-4j/pull/1054) ([stevehu](https://github.com/stevehu))
- Issue 1048 [\#1051](https://github.com/networknt/light-4j/pull/1051) ([stevehu](https://github.com/stevehu))
- max json payload for proxy which using buffer stream [\#1050](https://github.com/networknt/light-4j/pull/1050) ([GavinChenYan](https://github.com/GavinChenYan))
- fixes \#1048 update ProxyBodyHandler to handle the data form and add t… [\#1049](https://github.com/networknt/light-4j/pull/1049) ([stevehu](https://github.com/stevehu))
- add other contentType for proxy body handler [\#1047](https://github.com/networknt/light-4j/pull/1047) ([GavinChenYan](https://github.com/GavinChenYan))


## [2.0.32](https://github.com/networknt/light-4j/tree/2.0.32) (2021-10-19)


**Merged pull requests:**


- fixes \#1045 add checkInterval to the TTL check body to find the right [\#1046](https://github.com/networknt/light-4j/pull/1046) ([stevehu](https://github.com/stevehu))
- Feature/get log contents [\#1044](https://github.com/networknt/light-4j/pull/1044) ([KalevGonvick](https://github.com/KalevGonvick))
- fixes \#1042 create a TimeUtil in the utility module for scheduler and… [\#1043](https://github.com/networknt/light-4j/pull/1043) ([stevehu](https://github.com/stevehu))
-  java.lang.IllegalArgumentException: Label cannot be null. \#1039  [\#1040](https://github.com/networknt/light-4j/pull/1040) ([helloalbin](https://github.com/helloalbin))
- Added handler for grabbing log contents [\#1041](https://github.com/networknt/light-4j/pull/1041) ([KalevGonvick](https://github.com/KalevGonvick))
- fixes \#1037 A typo in the deregister url for the protal registry [\#1038](https://github.com/networknt/light-4j/pull/1038) ([stevehu](https://github.com/stevehu))
-  fixes \#1035 [\#1036](https://github.com/networknt/light-4j/pull/1036) ([GavinChenYan](https://github.com/GavinChenYan))


## [2.0.31](https://github.com/networknt/light-4j/tree/2.0.31) (2021-09-22)


**Merged pull requests:**


-  fixes \#1033 [\#1034](https://github.com/networknt/light-4j/pull/1034) ([GavinChenYan](https://github.com/GavinChenYan))
- add default header when client request missed the setting [\#1032](https://github.com/networknt/light-4j/pull/1032) ([GavinChenYan](https://github.com/GavinChenYan))
- fixes \#1030 update the portal de-registry to add checkInterval to the… [\#1031](https://github.com/networknt/light-4j/pull/1031) ([stevehu](https://github.com/stevehu))
- fixes \#1026 [\#1027](https://github.com/networknt/light-4j/pull/1027) ([GavinChenYan](https://github.com/GavinChenYan))
- add caller id header to audit map which will be used metrics [\#1025](https://github.com/networknt/light-4j/pull/1025) ([GavinChenYan](https://github.com/GavinChenYan))
## [2.0.30](https://github.com/networknt/light-4j/tree/2.0.30) (2021-08-23)


**Merged pull requests:**


-  fixes \#1023 [\#1024](https://github.com/networknt/light-4j/pull/1024) ([GavinChenYan](https://github.com/GavinChenYan))
-  add status change and new method for Result object [\#1022](https://github.com/networknt/light-4j/pull/1022) ([GavinChenYan](https://github.com/GavinChenYan))
- Add new error code [\#1021](https://github.com/networknt/light-4j/pull/1021) ([wswjwjccjlu](https://github.com/wswjwjccjlu))
-  fixes \#1019 [\#1020](https://github.com/networknt/light-4j/pull/1020) ([GavinChenYan](https://github.com/GavinChenYan))
- issue\#1017 add new error codes [\#1018](https://github.com/networknt/light-4j/pull/1018) ([wswjwjccjlu](https://github.com/wswjwjccjlu))
-  fixes \#1015 [\#1016](https://github.com/networknt/light-4j/pull/1016) ([GavinChenYan](https://github.com/GavinChenYan))
- Issue1011 [\#1014](https://github.com/networknt/light-4j/pull/1014) ([stevehu](https://github.com/stevehu))
- change egress router to allow user to extend the router handler [\#1013](https://github.com/networknt/light-4j/pull/1013) ([GavinChenYan](https://github.com/GavinChenYan))
- fixes \#1011 update the health.yml to enable downstream health check w… [\#1012](https://github.com/networknt/light-4j/pull/1012) ([stevehu](https://github.com/stevehu))
- fixes \#1009 update config server path with config-server base path [\#1010](https://github.com/networknt/light-4j/pull/1010) ([stevehu](https://github.com/stevehu))
## [2.0.29](https://github.com/networknt/light-4j/tree/2.0.29) (2021-07-25)


**Merged pull requests:**


-  add two constants for http-sidecar usage [\#1008](https://github.com/networknt/light-4j/pull/1008) ([chenyan71](https://github.com/chenyan71))
- Issue1006 [\#1007](https://github.com/networknt/light-4j/pull/1007) ([chenyan71](https://github.com/chenyan71))
-  build error fix, change egress_router value scope [\#1005](https://github.com/networknt/light-4j/pull/1005) ([chenyan71](https://github.com/chenyan71))
- fixes \#1003 audit request body serialize to JSON if possible and fall… [\#1004](https://github.com/networknt/light-4j/pull/1004) ([stevehu](https://github.com/stevehu))
-  fix an issue on jwt.yml config file [\#1002](https://github.com/networknt/light-4j/pull/1002) ([chenyan71](https://github.com/chenyan71))
- fixes \#1000 support both X509Certificate and JsonWebKeySet at the sam… [\#1001](https://github.com/networknt/light-4j/pull/1001) ([stevehu](https://github.com/stevehu))
- fixes \#998 add portalToken to the portal-registry.yml and use the tok… [\#999](https://github.com/networknt/light-4j/pull/999) ([stevehu](https://github.com/stevehu))
- fixes \#995 [\#996](https://github.com/networknt/light-4j/pull/996) ([chenyan71](https://github.com/chenyan71))
- fixes \#993 add a test case to generate a bootstrap token for service … [\#994](https://github.com/networknt/light-4j/pull/994) ([stevehu](https://github.com/stevehu))
- fixes \#991 add a status code for the service claim mismatch to the path [\#992](https://github.com/networknt/light-4j/pull/992) ([stevehu](https://github.com/stevehu))
- fixes \#989 trim the environment variable for config server uri and co… [\#990](https://github.com/networknt/light-4j/pull/990) ([stevehu](https://github.com/stevehu))
- Fix the NPEs in Issues 962 and 981 [\#988](https://github.com/networknt/light-4j/pull/988) ([containerAnalyzer](https://github.com/containerAnalyzer))
- fixes \#986 add a new error code for oauth to indicate that the author… [\#987](https://github.com/networknt/light-4j/pull/987) ([stevehu](https://github.com/stevehu))
- fixes \#984 [\#985](https://github.com/networknt/light-4j/pull/985) ([chenyan71](https://github.com/chenyan71))
- fixes \#982 disable loadConfigs from DefaultConfigLoader [\#983](https://github.com/networknt/light-4j/pull/983) ([stevehu](https://github.com/stevehu))
- fixes \#979 lazy creation of the jdk11 http client to connect to confi… [\#980](https://github.com/networknt/light-4j/pull/980) ([stevehu](https://github.com/stevehu))
- fixes \#970 [\#975](https://github.com/networknt/light-4j/pull/975) ([chenyan71](https://github.com/chenyan71))
- fixes \#977 output content of the config files from config server in log [\#978](https://github.com/networknt/light-4j/pull/978) ([stevehu](https://github.com/stevehu))
- fixes \#973 switch to jdk 11 http client to connect to the config server [\#974](https://github.com/networknt/light-4j/pull/974) ([stevehu](https://github.com/stevehu))
- fixes \#971 remove the server.config and switch to getServerConfig method [\#972](https://github.com/networknt/light-4j/pull/972) ([stevehu](https://github.com/stevehu))


## [2.0.28](https://github.com/networknt/light-4j/tree/2.0.28) (2021-06-27)


**Merged pull requests:**


- fixes \#965 [\#969](https://github.com/networknt/light-4j/pull/969) ([chenyan71](https://github.com/chenyan71))
- fixes \#967 make the jaeger-client optional in the client module [\#968](https://github.com/networknt/light-4j/pull/968) ([stevehu](https://github.com/stevehu))
- fixes \#963 update the logic to support both http and https port cache… [\#964](https://github.com/networknt/light-4j/pull/964) ([stevehu](https://github.com/stevehu))
- fixes \#960 support both environment variable and jvm option for confi… [\#961](https://github.com/networknt/light-4j/pull/961) ([stevehu](https://github.com/stevehu))
- fixes \#958 add a new error code to the status.yml to indicate kafka-s… [\#959](https://github.com/networknt/light-4j/pull/959) ([stevehu](https://github.com/stevehu))
- fixes \#956 add registerModule for the SingletonServiceFactory [\#957](https://github.com/networknt/light-4j/pull/957) ([stevehu](https://github.com/stevehu))
- fixes \#954 remove the specification section from the server info resp… [\#955](https://github.com/networknt/light-4j/pull/955) ([stevehu](https://github.com/stevehu))
- fixes \#952 upgrade jaeger to 1.6.0 to resolve security vulnerabilities [\#953](https://github.com/networknt/light-4j/pull/953) ([stevehu](https://github.com/stevehu))
## [2.0.27](https://github.com/networknt/light-4j/tree/2.0.27) (2021-05-25)


**Merged pull requests:**


- fixes \#950 make the body.yml externalizable with the values.yml [\#951](https://github.com/networknt/light-4j/pull/951) ([stevehu](https://github.com/stevehu))
- url config loader [\#947](https://github.com/networknt/light-4j/pull/947) ([xlongwei](https://github.com/xlongwei))
- fixes \#948 Add ProxyBodyHandler for sidecar to intercept the body [\#949](https://github.com/networknt/light-4j/pull/949) ([stevehu](https://github.com/stevehu))
- fixes \#942 handle only one element in an array for masking [\#946](https://github.com/networknt/light-4j/pull/946) ([stevehu](https://github.com/stevehu))
- Issue942 [\#945](https://github.com/networknt/light-4j/pull/945) ([ssoifer](https://github.com/ssoifer))
- fixes \#943 update client.yml to move the OAuth token proxyHost and pr… [\#944](https://github.com/networknt/light-4j/pull/944) ([stevehu](https://github.com/stevehu))
- fixes \#940 update OauthHelper getTokenResult to check before apply pr… [\#941](https://github.com/networknt/light-4j/pull/941) ([stevehu](https://github.com/stevehu))
- fixes \#938 add healthPath to the portalRegistryService for controller [\#939](https://github.com/networknt/light-4j/pull/939) ([stevehu](https://github.com/stevehu))
## [2.0.26](https://github.com/networknt/light-4j/tree/2.0.26) (2021-04-27)


**Merged pull requests:**


- added base path in the HandlerConfig [\#934](https://github.com/networknt/light-4j/pull/934) ([BalloonWen](https://github.com/BalloonWen))
- fixes \#932 make the health check path configurable in the portal-regi… [\#933](https://github.com/networknt/light-4j/pull/933) ([stevehu](https://github.com/stevehu))
- fixes \#929 add error codes for the light-mesh kafka-sidecar [\#930](https://github.com/networknt/light-4j/pull/930) ([stevehu](https://github.com/stevehu))
## [2.0.25](https://github.com/networknt/light-4j/tree/2.0.25) (2021-03-28)


**Merged pull requests:**


- mask server info sensitive data [\#927](https://github.com/networknt/light-4j/pull/927) ([BalloonWen](https://github.com/BalloonWen))
- Issue924 [\#925](https://github.com/networknt/light-4j/pull/925) ([stevehu](https://github.com/stevehu))
- combine proxy info and target servers info [\#923](https://github.com/networknt/light-4j/pull/923) ([BalloonWen](https://github.com/BalloonWen))


## [2.0.24](https://github.com/networknt/light-4j/tree/2.0.24) (2021-02-24)


**Merged pull requests:**


- fixes \#920 update CookiesDumper in DumpHandler after upgrade to under… [\#921](https://github.com/networknt/light-4j/pull/921) ([stevehu](https://github.com/stevehu))
- Bump version.jackson from 2.10.4 to 2.12.1 [\#919](https://github.com/networknt/light-4j/pull/919) ([dependabot](https://github.com/apps/dependabot))
- issue \#897 key resolving at the start up [\#918](https://github.com/networknt/light-4j/pull/918) ([BalloonWen](https://github.com/BalloonWen))
- fixes \#916 register the handler and server modules to server info [\#917](https://github.com/networknt/light-4j/pull/917) ([stevehu](https://github.com/stevehu))
- fixes \#914 move the getFileExtension from light-codegen to the NioUti… [\#915](https://github.com/networknt/light-4j/pull/915) ([stevehu](https://github.com/stevehu))
- allow key injection in configuration [\#913](https://github.com/networknt/light-4j/pull/913) ([BalloonWen](https://github.com/BalloonWen))
- issue \#898 log err when get oauth key exception [\#911](https://github.com/networknt/light-4j/pull/911) ([BalloonWen](https://github.com/BalloonWen))
- fixes \#909 make shutdown timeout and shutdown graceful period configu… [\#910](https://github.com/networknt/light-4j/pull/910) ([stevehu](https://github.com/stevehu))
- fixes \#906 remove primary and secondary jks from security resources/c… [\#907](https://github.com/networknt/light-4j/pull/907) ([stevehu](https://github.com/stevehu))
## [2.0.23](https://github.com/networknt/light-4j/tree/2.0.23) (2021-01-29)


**Merged pull requests:**


- issue \#904   fix message is not displayed by default [\#905](https://github.com/networknt/light-4j/pull/905) ([BalloonWen](https://github.com/BalloonWen))
- fixes \#902 add an error code for light-chaos-monkey [\#903](https://github.com/networknt/light-4j/pull/903) ([stevehu](https://github.com/stevehu))
- revert issue \#898 [\#901](https://github.com/networknt/light-4j/pull/901) ([BalloonWen](https://github.com/BalloonWen))
- Feat/\#898 oauth key [\#900](https://github.com/networknt/light-4j/pull/900) ([BalloonWen](https://github.com/BalloonWen))
- Feat/\#898 oauth key [\#899](https://github.com/networknt/light-4j/pull/899) ([BalloonWen](https://github.com/BalloonWen))
- Fixing other ranges [\#896](https://github.com/networknt/light-4j/pull/896) ([jaswalkiranavtar](https://github.com/jaswalkiranavtar))
- issue \#894 show metadaata, description, message separately [\#895](https://github.com/networknt/light-4j/pull/895) ([BalloonWen](https://github.com/BalloonWen))
- Fixing range to make it consistent with others [\#893](https://github.com/networknt/light-4j/pull/893) ([jaswalkiranavtar](https://github.com/jaswalkiranavtar))
- Feat/\#884 add metadata to Status [\#889](https://github.com/networknt/light-4j/pull/889) ([BalloonWen](https://github.com/BalloonWen))
- fixes \#883 output an error message in the service module if the implm… [\#892](https://github.com/networknt/light-4j/pull/892) ([stevehu](https://github.com/stevehu))
- fixes \#890 move the ByteUtil from light-portal to light-4j utility mo… [\#891](https://github.com/networknt/light-4j/pull/891) ([stevehu](https://github.com/stevehu))
- Fix for 887 [\#888](https://github.com/networknt/light-4j/pull/888) ([helloalbin](https://github.com/helloalbin))
- fixes \#885 add two status codes for lambda invoker handler for light-… [\#886](https://github.com/networknt/light-4j/pull/886) ([stevehu](https://github.com/stevehu))
- Fix/\#876  LightHttpHandler is getting auditOnError and auditStackTrace config properly. [\#882](https://github.com/networknt/light-4j/pull/882) ([BalloonWen](https://github.com/BalloonWen))
## [2.0.22](https://github.com/networknt/light-4j/tree/2.0.22) (2020-12-22)


**Merged pull requests:**


- fixes \#880 add two status codes for light-aws-lambda [\#881](https://github.com/networknt/light-4j/pull/881) ([stevehu](https://github.com/stevehu))
- issue \#877 [\#878](https://github.com/networknt/light-4j/pull/878) ([BalloonWen](https://github.com/BalloonWen))
- issue-\#874 [\#875](https://github.com/networknt/light-4j/pull/875) ([BalloonWen](https://github.com/BalloonWen))
- fixes \#872 add tId to the MDC from TraceabilityHandler so that it can… [\#873](https://github.com/networknt/light-4j/pull/873) ([stevehu](https://github.com/stevehu))
- fixes \#870 add primary_scopes and secondary_scopes to constants [\#871](https://github.com/networknt/light-4j/pull/871) ([stevehu](https://github.com/stevehu))
- fixes \#868 disable the audit on requestBody and responseBody audit in… [\#869](https://github.com/networknt/light-4j/pull/869) ([stevehu](https://github.com/stevehu))
- fixes \#866 move TlsUtil to config module from utility to remove confi… [\#867](https://github.com/networknt/light-4j/pull/867) ([stevehu](https://github.com/stevehu))
- \#841 added StatusWrapper interface [\#859](https://github.com/networknt/light-4j/pull/859) ([jiachen1120](https://github.com/jiachen1120))
- issue-864 seprate logging error status and stack trace [\#865](https://github.com/networknt/light-4j/pull/865) ([BalloonWen](https://github.com/BalloonWen))
- issue-858 auditing timestamp supports custome date format [\#860](https://github.com/networknt/light-4j/pull/860) ([BalloonWen](https://github.com/BalloonWen))
- Fix/861 config load exception when auditing service id [\#863](https://github.com/networknt/light-4j/pull/863) ([BalloonWen](https://github.com/BalloonWen))
- Add missing package import in PortalRegistryTest.java [\#862](https://github.com/networknt/light-4j/pull/862) ([KellyShao](https://github.com/KellyShao))
- Fix flaky tests in PortalRegistryTest.java and ConsulRegistryTest.java [\#857](https://github.com/networknt/light-4j/pull/857) ([KellyShao](https://github.com/KellyShao))
- Fixed flaky tests caused by SharedMetricRegistries in SharedMetricRegistriesTest.java [\#856](https://github.com/networknt/light-4j/pull/856) ([KellyShao](https://github.com/KellyShao))
- - solved \#840 [\#849](https://github.com/networknt/light-4j/pull/849) ([jiachen1120](https://github.com/jiachen1120))
- fixes \#854 RoundRobinLoadBalance is unpredictable if there are more t… [\#855](https://github.com/networknt/light-4j/pull/855) ([stevehu](https://github.com/stevehu))
- fixes \#852 parameterize health and info configuration file [\#853](https://github.com/networknt/light-4j/pull/853) ([stevehu](https://github.com/stevehu))
- fixes \#850 add an error code for the logger post if the body does not… [\#851](https://github.com/networknt/light-4j/pull/851) ([stevehu](https://github.com/stevehu))
- fixes \#847 add status code for the registry failure during the server… [\#848](https://github.com/networknt/light-4j/pull/848) ([stevehu](https://github.com/stevehu))
- fixes \#845 add startOnRegistryFailure to server.yml to control if sta… [\#846](https://github.com/networknt/light-4j/pull/846) ([stevehu](https://github.com/stevehu))
- fixes \#843 update logger-config for light-controller to query and upd… [\#844](https://github.com/networknt/light-4j/pull/844) ([stevehu](https://github.com/stevehu))
- - fixed \#839 Error status resulted at framework middleware handler is… [\#842](https://github.com/networknt/light-4j/pull/842) ([jiachen1120](https://github.com/jiachen1120))


## [2.0.21](https://github.com/networknt/light-4j/tree/2.0.21) (2020-11-25)


**Merged pull requests:**


- fixes \#837 invoke notify only if listener is not null in the direct r… [\#838](https://github.com/networknt/light-4j/pull/838) ([stevehu](https://github.com/stevehu))
- fixes \#835 add CID and UID string into the constants for Okta token [\#836](https://github.com/networknt/light-4j/pull/836) ([stevehu](https://github.com/stevehu))
- fixes \#833 subscribe and unsubscribe regardless listener for Abstract… [\#834](https://github.com/networknt/light-4j/pull/834) ([stevehu](https://github.com/stevehu))
- fixes \#829 remove the serviceMap cache from the LightCluster and use … [\#832](https://github.com/networknt/light-4j/pull/832) ([stevehu](https://github.com/stevehu))
- fixes \#830 remove zookeeper registry as nobody is using it [\#831](https://github.com/networknt/light-4j/pull/831) ([stevehu](https://github.com/stevehu))
- fixes \#827 update discovered cache with WebSocket message from contro… [\#828](https://github.com/networknt/light-4j/pull/828) ([stevehu](https://github.com/stevehu))
- fixes \#825 update the service lookup for the Portal Registry [\#826](https://github.com/networknt/light-4j/pull/826) ([stevehu](https://github.com/stevehu))
- fixes \#823 add a new test case for JwtIssuerTest to generate light-pr… [\#824](https://github.com/networknt/light-4j/pull/824) ([stevehu](https://github.com/stevehu))
- Issue821 [\#822](https://github.com/networknt/light-4j/pull/822) ([stevehu](https://github.com/stevehu))
- fixes \#819 output the body in the ServerInfoGetHandlerTest as it fail… [\#820](https://github.com/networknt/light-4j/pull/820) ([stevehu](https://github.com/stevehu))
- fixes \#817 update the currentPort and currentAddress in server module [\#818](https://github.com/networknt/light-4j/pull/818) ([stevehu](https://github.com/stevehu))
- Issue815 [\#816](https://github.com/networknt/light-4j/pull/816) ([stevehu](https://github.com/stevehu))
- fixes \#813 update portal-registry.yml with templates in both main and… [\#814](https://github.com/networknt/light-4j/pull/814) ([stevehu](https://github.com/stevehu))
- fixes \#811 upgrade server info handler to output openapi.yaml and upd… [\#812](https://github.com/networknt/light-4j/pull/812) ([stevehu](https://github.com/stevehu))
- fixes \#809 update handler to support websocket in the handler.yml [\#810](https://github.com/networknt/light-4j/pull/810) ([stevehu](https://github.com/stevehu))
- fixes \#807 add portal-registry model for portal registry and discovery [\#808](https://github.com/networknt/light-4j/pull/808) ([stevehu](https://github.com/stevehu))
- fixes \#805 update server.yml to add keystore and truststore passwords [\#806](https://github.com/networknt/light-4j/pull/806) ([stevehu](https://github.com/stevehu))
- fixes \#803 update client.yml to add truststore password [\#804](https://github.com/networknt/light-4j/pull/804) ([stevehu](https://github.com/stevehu))
- fixes \#801 add password to the jwt.yml as the secret.yml is removed [\#802](https://github.com/networknt/light-4j/pull/802) ([stevehu](https://github.com/stevehu))
- fixes \#799 remove secret.yml and add config property missing error code [\#800](https://github.com/networknt/light-4j/pull/800) ([stevehu](https://github.com/stevehu))
- fixes \#775 log the connection info when failed to connect to the OAut… [\#798](https://github.com/networknt/light-4j/pull/798) ([stevehu](https://github.com/stevehu))
- fixes \#796 update the redirect_uri to localhost 3000 to sync with lig… [\#797](https://github.com/networknt/light-4j/pull/797) ([stevehu](https://github.com/stevehu))
- fixes \#794 switch OauthHelper to HttpClient of jdk11 to support forwa… [\#795](https://github.com/networknt/light-4j/pull/795) ([stevehu](https://github.com/stevehu))


## [2.0.20](https://github.com/networknt/light-4j/tree/2.0.20) (2020-11-05)


**Merged pull requests:**


- fixes \#792 add a scp constant string to enure there is no hard-coded … [\#793](https://github.com/networknt/light-4j/pull/793) ([stevehu](https://github.com/stevehu))
- fixes \#790 update security test to generate long lived tokens with sc… [\#791](https://github.com/networknt/light-4j/pull/791) ([stevehu](https://github.com/stevehu))
- fixes \#788 update the OAuth key request for jwk integration [\#789](https://github.com/networknt/light-4j/pull/789) ([stevehu](https://github.com/stevehu))
## [2.0.19](https://github.com/networknt/light-4j/tree/2.0.19) (2020-11-01)


**Merged pull requests:**


- Issue786 [\#787](https://github.com/networknt/light-4j/pull/787) ([stevehu](https://github.com/stevehu))
- fixes \#784 get server config without cache in the client module [\#785](https://github.com/networknt/light-4j/pull/785) ([stevehu](https://github.com/stevehu))
- fixes \#782 add startup.yml to the server module to suppress the warning [\#783](https://github.com/networknt/light-4j/pull/783) ([stevehu](https://github.com/stevehu))
- fixes \#780 Add callerId to the metrics collection with a header in th… [\#781](https://github.com/networknt/light-4j/pull/781) ([stevehu](https://github.com/stevehu))
- fixes \#778 add scopeClientId to the metrics handler [\#779](https://github.com/networknt/light-4j/pull/779) ([stevehu](https://github.com/stevehu))
- Bump junit from 4.12 to 4.13.1 [\#777](https://github.com/networknt/light-4j/pull/777) ([dependabot](https://github.com/apps/dependabot))
- Avoid NoSuchElementException in ManualAESDecryptor. [\#776](https://github.com/networknt/light-4j/pull/776) ([rgrig](https://github.com/rgrig))
## [2.0.18](https://github.com/networknt/light-4j/tree/2.0.18) (2020-10-01)


**Merged pull requests:**


- fixes \#773 resolve a defect to handle the environment tag with single… [\#774](https://github.com/networknt/light-4j/pull/774) ([stevehu](https://github.com/stevehu))
- fixes \#771 change the STATUS_HOST_IP to public [\#772](https://github.com/networknt/light-4j/pull/772) ([stevehu](https://github.com/stevehu))
- fixes \#769 add environment tag to the DirectRegistry to support tag b… [\#770](https://github.com/networknt/light-4j/pull/770) ([stevehu](https://github.com/stevehu))
- fixes \#765 add email registered error to the status.yml for light-rou… [\#766](https://github.com/networknt/light-4j/pull/766) ([stevehu](https://github.com/stevehu))
- fixes \#763 add client authenticated user request in the OauthHelper f… [\#764](https://github.com/networknt/light-4j/pull/764) ([stevehu](https://github.com/stevehu))
- fixes \#761 capture the ip address and port number on server for metrics [\#762](https://github.com/networknt/light-4j/pull/762) ([stevehu](https://github.com/stevehu))


## [2.0.17](https://github.com/networknt/light-4j/tree/2.0.17) (2020-08-28)


**Merged pull requests:**


- fixes \#759 skip sanitizer if the body is not JSON [\#760](https://github.com/networknt/light-4j/pull/760) ([stevehu](https://github.com/stevehu))
- fixes \#757 avoid parsing the body if content-type is missing [\#758](https://github.com/networknt/light-4j/pull/758) ([stevehu](https://github.com/stevehu))
- fixes \#753 handle text/plain and content type missing in BodyHandler [\#755](https://github.com/networknt/light-4j/pull/755) ([stevehu](https://github.com/stevehu))
- fixes \#754 Add content type text/plain header to the InfluxDbSender [\#756](https://github.com/networknt/light-4j/pull/756) ([stevehu](https://github.com/stevehu))
- Issue 742 [\#749](https://github.com/networknt/light-4j/pull/749) ([chenyan71](https://github.com/chenyan71))
- fixes \#751 add a method to create client credentials token in issuer … [\#752](https://github.com/networknt/light-4j/pull/752) ([stevehu](https://github.com/stevehu))
- fixes \#747 Add a generic status code for 404 object not found [\#748](https://github.com/networknt/light-4j/pull/748) ([stevehu](https://github.com/stevehu))
## [2.0.16](https://github.com/networknt/light-4j/tree/2.0.16) (2020-08-01)


**Merged pull requests:**


- fixes \#743 Handle the limit file size for upload/download [\#746](https://github.com/networknt/light-4j/pull/746) ([stevehu](https://github.com/stevehu))
- Issue \#744: Valuemap not pickup the values loaded from config server [\#745](https://github.com/networknt/light-4j/pull/745) ([jsu216](https://github.com/jsu216))
- fixes \#740 check escape backslash before appy quoteReplacement config [\#741](https://github.com/networknt/light-4j/pull/741) ([stevehu](https://github.com/stevehu))
- fixes \#723 Token replacement in config files does not allow special c… [\#738](https://github.com/networknt/light-4j/pull/738) ([stevehu](https://github.com/stevehu))
-  fixes \#710 [\#739](https://github.com/networknt/light-4j/pull/739) ([chenyan71](https://github.com/chenyan71))
- fixes \#735 update client.yml in test resources to disable http2 as th… [\#736](https://github.com/networknt/light-4j/pull/736) ([stevehu](https://github.com/stevehu))
- fixes \#721 update consul client to leverage connecction pool of Http2… [\#722](https://github.com/networknt/light-4j/pull/722) ([stevehu](https://github.com/stevehu))
- fixes \#718 add borrowConnection and returnConnection to Http2Client [\#720](https://github.com/networknt/light-4j/pull/720) ([stevehu](https://github.com/stevehu))
## [2.0.15](https://github.com/networknt/light-4j/tree/2.0.15) (2020-07-01)


**Merged pull requests:**


- fixes \#732 add several status code to status.yml for reference API in… [\#733](https://github.com/networknt/light-4j/pull/733) ([stevehu](https://github.com/stevehu))
-  fixes \#730 [\#731](https://github.com/networknt/light-4j/pull/731) ([chenyan71](https://github.com/chenyan71))
-  no change to index same [\#729](https://github.com/networknt/light-4j/pull/729) ([chenyan71](https://github.com/chenyan71))
- fixes \#726 [\#727](https://github.com/networknt/light-4j/pull/727) ([chenyan71](https://github.com/chenyan71))
- fixes \#724 remove the temporary version.jackson-databind in the pom.xml [\#725](https://github.com/networknt/light-4j/pull/725) ([stevehu](https://github.com/stevehu))
- fix for issue \#715 - ensure cached consul connections are specific to… [\#716](https://github.com/networknt/light-4j/pull/716) ([miklish](https://github.com/miklish))
- fixes \#713 reformat the BodyConverter and add the missing import [\#714](https://github.com/networknt/light-4j/pull/714) ([stevehu](https://github.com/stevehu))
- Update BodyConverter.java [\#712](https://github.com/networknt/light-4j/pull/712) ([thirtysixmm](https://github.com/thirtysixmm))
## [2.0.14](https://github.com/networknt/light-4j/tree/2.0.14) (2020-05-29)


**Merged pull requests:**


- fixes \#709 [\#711](https://github.com/networknt/light-4j/pull/711) ([chenyan71](https://github.com/chenyan71))
- fixes \#707 switch toByteBuffer from direct to indirect [\#708](https://github.com/networknt/light-4j/pull/708) ([stevehu](https://github.com/stevehu))
- fixes \#704 add keyResolver to the security.yml with comment [\#705](https://github.com/networknt/light-4j/pull/705) ([stevehu](https://github.com/stevehu))
## [2.0.13](https://github.com/networknt/light-4j/tree/2.0.13) (2020-05-01)


**Merged pull requests:**


- fixes \#699 add start_time to refresh_token table with default [\#700](https://github.com/networknt/light-4j/pull/700) ([stevehu](https://github.com/stevehu))
- fixes \#696 add remember to token response to handle refresh token [\#697](https://github.com/networknt/light-4j/pull/697) ([stevehu](https://github.com/stevehu))
- fixes \#694 double check the tracer has activeSpan in the client heade… [\#695](https://github.com/networknt/light-4j/pull/695) ([stevehu](https://github.com/stevehu))
- fixes \#692 add host and port to the JaegerHandler for tracing [\#693](https://github.com/networknt/light-4j/pull/693) ([stevehu](https://github.com/stevehu))
- fixes \#690 add test case to email-sender to demo the environment vari… [\#691](https://github.com/networknt/light-4j/pull/691) ([stevehu](https://github.com/stevehu))
## [2.0.12](https://github.com/networknt/light-4j/tree/2.0.12) (2020-03-31)


**Merged pull requests:**


- fixes \#688 add status codes for light-portal [\#689](https://github.com/networknt/light-4j/pull/689) ([stevehu](https://github.com/stevehu))
- fixes \#686 apply issue 679 to master branch [\#687](https://github.com/networknt/light-4j/pull/687) ([stevehu](https://github.com/stevehu))
- fixes \#683 update master based on 677 and 681 [\#685](https://github.com/networknt/light-4j/pull/685) ([stevehu](https://github.com/stevehu))
- fixes \#675 resolve a defect in client.yml for the oauth2 urls [\#676](https://github.com/networknt/light-4j/pull/676) ([stevehu](https://github.com/stevehu))
- fixes \#673 add replaceToken method for html email template [\#674](https://github.com/networknt/light-4j/pull/674) ([stevehu](https://github.com/stevehu))
- fixes \#671 change Http2Client createClientCallback debug to trace [\#672](https://github.com/networknt/light-4j/pull/672) ([stevehu](https://github.com/stevehu))
- fixes \#669 parameterize default client.yml in client module [\#670](https://github.com/networknt/light-4j/pull/670) ([stevehu](https://github.com/stevehu))
- fixes \#667 comment out server_url in client.yml to default to service… [\#668](https://github.com/networknt/light-4j/pull/668) ([stevehu](https://github.com/stevehu))
- fixes \#665 need to promote the scope token to authorization header if… [\#666](https://github.com/networknt/light-4j/pull/666) ([stevehu](https://github.com/stevehu))
- fixes \#663 update security test cases to add roles to the long-lived … [\#664](https://github.com/networknt/light-4j/pull/664) ([stevehu](https://github.com/stevehu))
## [2.0.11](https://github.com/networknt/light-4j/tree/2.0.11) (2020-02-29)


**Merged pull requests:**


- fixes \#658 add trace logging to help debug cors rejections in CorsUtil [\#659](https://github.com/networknt/light-4j/pull/659) ([stevehu](https://github.com/stevehu))
- fixes \#655 update logging level to trace for consul module [\#656](https://github.com/networknt/light-4j/pull/656) ([stevehu](https://github.com/stevehu))
-  fixes for \#649 [\#652](https://github.com/networknt/light-4j/pull/652) ([stevehu](https://github.com/stevehu))
- fixes \#653 update OauthHelper to allow redirectUri optional when gett… [\#654](https://github.com/networknt/light-4j/pull/654) ([stevehu](https://github.com/stevehu))
- fixes \#647 Service registration fails with the latest Consul [\#651](https://github.com/networknt/light-4j/pull/651) ([stevehu](https://github.com/stevehu))
- Issue645 [\#648](https://github.com/networknt/light-4j/pull/648) ([stevehu](https://github.com/stevehu))
- fixes \#645 loose the condition to apply the cors headers [\#646](https://github.com/networknt/light-4j/pull/646) ([stevehu](https://github.com/stevehu))
- fixes \#643 Add user_type and roles constants in utility for light-spa-4j [\#644](https://github.com/networknt/light-4j/pull/644) ([stevehu](https://github.com/stevehu))
## [2.0.10](https://github.com/networknt/light-4j/tree/2.0.10) (2020-01-31)


**Merged pull requests:**


- fixes \#641 remove oracle JDBC driver test dependency [\#642](https://github.com/networknt/light-4j/pull/642) ([stevehu](https://github.com/stevehu))
- fixes \#639 [\#640](https://github.com/networknt/light-4j/pull/640) ([chenyan71](https://github.com/chenyan71))


## [2.0.9](https://github.com/networknt/light-4j/tree/2.0.9) (2019-12-30)


**Merged pull requests:**


- fixes \#637 refactor the LightHttpHandler to ensure the auditInfo is c… [\#638](https://github.com/networknt/light-4j/pull/638) ([stevehu](https://github.com/stevehu))
- fixes \#635 add a test case with two generic types for service module [\#636](https://github.com/networknt/light-4j/pull/636) ([stevehu](https://github.com/stevehu))
- Bug fix: ConsulRegistry keeps the first service url discovered in cache [\#633](https://github.com/networknt/light-4j/pull/633) ([jsu216](https://github.com/jsu216))
- fixes issue \#625 [\#626](https://github.com/networknt/light-4j/pull/626) ([chenyan71](https://github.com/chenyan71))
- Make Http2Client.SSL public again, but deprecate it [\#631](https://github.com/networknt/light-4j/pull/631) ([miklish](https://github.com/miklish))
- Make SSL private to prevent its usage before it is initialized.  [\#630](https://github.com/networknt/light-4j/pull/630) ([miklish](https://github.com/miklish))
- Make valueMap to be static to prevent multiple warning [\#628](https://github.com/networknt/light-4j/pull/628) ([jiachen1120](https://github.com/jiachen1120))
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
