package com.networknt.rule;

import com.networknt.client.ClientConfig;
import com.networknt.client.Http2Client;
import com.networknt.client.simplepool.SimpleConnectionHolder;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.monad.Success;
import com.networknt.server.DefaultConfigLoader;
import com.networknt.server.Server;
import com.networknt.server.ServerConfig;
import com.networknt.server.StartupHookProvider;
import com.networknt.status.Status;
import com.networknt.status.Status;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static com.networknt.server.Server.STARTUP_CONFIG_NAME;


/**
 * This is the startup hook to load YAML rules from the light-portal during the server startup. Currently,
 * only the access-control is using the rule engine; however, we are expecting more components will follow.
 *
 * @author Steve Hu
 */
public class RuleLoaderStartupHook implements StartupHookProvider {
    static final Logger logger = LoggerFactory.getLogger(RuleLoaderStartupHook.class);
    public static final String MASK_PORTAL_TOKEN = "portalToken";

    // shared rule map with ruleId as the key and Rule object as the value.
    public static Map<String, Object> endpointRules;
    public static Map<String, Rule> rules;
    static Http2Client client = Http2Client.getInstance();
    static final String GENERIC_EXCEPTION = "ERR10014";
    static final String DEFAULT_HOST = "lightapi.net";
    // As this startup hook is a singleton, we can use a static variable to hold the rule engine.
    public static RuleEngine ruleEngine;

    @Override
    public void onStartup() {
        RuleLoaderConfig config = RuleLoaderConfig.load();
        if(config.isEnabled()) {
            // by default the rules for the service is loaded from the light-portal; however, it can be configured to loaded from config folder.
            if(RuleLoaderConfig.RULE_SOURCE_CONFIG_FOLDER.equals(config.getRuleSource())) {
                // load the rules for the service from the externalized config folder. The filename is rules.yml
                String ruleString = Config.getInstance().getStringFromFile("rules.yml");
                rules = RuleMapper.string2RuleMap(ruleString);
                if(logger.isInfoEnabled())
                    logger.info("Load YAML rules from config folder with size = {}", rules.size());
                // load the endpoint rule mapping from the rule-loader.yml
                endpointRules = config.getEndpointRules();
            } else {
                // by default, load from light-portal
                Map<String, Object> startupConfig = Config.getInstance().getJsonMapConfig(Server.STARTUP_CONFIG_NAME);
                String hostId = (String)startupConfig.get("hostId");
                String apiId = (String)startupConfig.get("apiId");
                String apiVersion = (String)startupConfig.get("apiVersion");
                Result<String> result = getServiceRule(config.getPortalHost(), hostId, apiId, apiVersion);
                if(result.isSuccess()) {
                    String serviceRuleString = result.getResult();
                    if(logger.isDebugEnabled()) logger.debug("getServiceRule result = {}", serviceRuleString);
                    Map<String, Object> objectMap = JsonMapper.string2Map(serviceRuleString);
                    List<Map<String, Object>> ruleList = (List<Map<String, Object>>)objectMap.get("rules");
                    // TODO move to config server for persistence in values.yml.
                    endpointRules = convertRuleList(ruleList);
                    if(logger.isTraceEnabled()) logger.trace("endpointRules = " + JsonMapper.toJson(endpointRules));
                    // enrich the endpointRules to add permission for each endpoint.
                    result = getServicePermission(config.getPortalHost(), hostId, apiId, apiVersion);
                    if(result.isSuccess()) {
                        String servicePermissionString = result.getResult();
                        if(logger.isDebugEnabled()) logger.debug("getServicePermission result = {}", servicePermissionString);
                        List<Map<String, Object>> permissions = JsonMapper.string2List(servicePermissionString);
                        for(Map<String, Object> permission: permissions) {
                            String endpoint = (String)permission.remove("endpoint");
                            // get the endpointRule from the endpointRules map.
                            Map<String, Object> endpointRule = (Map<String, Object>)endpointRules.get(endpoint);
                            if(endpointRule != null) {
                                endpointRule.put("permission", permission);
                            }
                        }
                    } else {
                        logger.error("Could not load permission for hostId {} apiId {} apiVersion {} error {}", hostId, apiId, apiVersion, result.getError());
                    }
                    Map<String, Object> ruleBodies = (Map<String, Object>)objectMap.get("ruleBodies");
                    // save ruleMap into rules.yml in case the portal server is not available.
                    String targetConfigsDirectory = DefaultConfigLoader.getTargetConfigsDirectory();
                    try {
                        Path filePath = Paths.get(targetConfigsDirectory);
                        if (!Files.exists(filePath)) {
                            Files.createDirectories(filePath);
                            logger.info("target configs directory {} created", targetConfigsDirectory);
                        }
                        DumperOptions options = new DumperOptions();
                        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);//to get yaml string without curly brackets and commas

                        filePath = Paths.get(targetConfigsDirectory + "/rules.yml");
                        String ruleString =  new Yaml(options).dump(ruleBodies);
                        rules = RuleMapper.string2RuleMap(ruleString);
                        Files.write(filePath, ruleString.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        logger.error("Exception while creating " + targetConfigsDirectory, e);
                    }
                    if(logger.isInfoEnabled())
                        logger.info("Load YAML rules from light-portal with size = {}", ruleBodies.size());
                } else {
                    logger.error("Could not load rule for hostId {} apiId {} apiVersion {} error {}", hostId, apiId, apiVersion, result.getError());
                }
            }
            if(rules != null) {
                // create the rule engine with the rule map.
                ruleEngine = new RuleEngine(rules, null);
                // iterate all action classes to initialize them to ensure that the jar file are deployed and configuration is registered.
                // This is to prevent runtime exception and also ensure that the configuration is part of the server info response.
                loadPluginClass();
            }
        } else {
            if(logger.isInfoEnabled()) logger.info("Rule Loader is not enabled and skipped loading rules from the portal.");
        }
    }

    public static void loadPluginClass() {
        // iterate the rules map to find the action classes.
        for(Rule rule: rules.values()) {
            if(rule.getActions() != null) {
                for (RuleAction action : rule.getActions()) {
                    String actionClass = action.getActionClassName();
                    loadActionClass(actionClass);
                }
            }
        }
    }
    public static void loadActionClass(String actionClass) {
        if(logger.isDebugEnabled()) logger.debug("load action class {}", actionClass);
        try {
            IAction ia = (IAction)Class.forName(actionClass).getDeclaredConstructor().newInstance();
            // this happens during the server startup, so the cache must be empty. No need to check.
            ruleEngine.actionClassCache.put(actionClass, ia);
        } catch (Exception e) {
            logger.error("Exception:", e);
            throw new RuntimeException("Could not find rule action class " + actionClass, e);
        }
    }

    private Map<String, Object> convertRuleList(List<Map<String, Object>> ruleList) {
        Map<String, Object> endpointRules = new HashMap<>();
        for(Map<String, Object> rule: ruleList) {
            String endpoint = (String)rule.get("endpoint");
            // check if the endpoint is already in the map.
            Map<String, Object> endpointRule = (Map<String, Object>)endpointRules.get(endpoint);
            if(endpointRule == null) {
                endpointRule = new HashMap<>();
                endpointRules.put(endpoint, endpointRule);
                List<String> ruleIds = new ArrayList<>();
                ruleIds.add((String)rule.get("ruleId"));
                endpointRule.put(rule.get("ruleType").toString(), ruleIds);
            } else {
                List<String> ruleIds = (List<String>)endpointRule.get(rule.get("ruleType").toString());
                if(ruleIds == null) {
                    ruleIds = new ArrayList<>();
                    ruleIds.add((String)rule.get("ruleId"));
                    endpointRule.put(rule.get("ruleType").toString(), ruleIds);
                } else {
                    ruleIds.add((String)rule.get("ruleId"));
                }
            }
        }
        return endpointRules;
    }

    public static Result<String> getServiceRule(String url, String hostId, String apiId, String apiVersion) {
        final String s = String.format("{\"host\":\"lightapi.net\",\"service\":\"market\",\"action\":\"getServiceRule\",\"version\":\"0.1.0\",\"data\":{\"hostId\":\"%s\",\"apiId\":\"%s\",\"apiVersion\":\"%s\"}}", hostId, apiId, apiVersion);
        Result<String> result = null;
        ClientConnection conn = null;
        try {
            SimpleConnectionHolder.ConnectionToken tokenConn = client.borrow(new URI(url), Http2Client.WORKER, client.getDefaultXnioSsl(), Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));

            conn = (ClientConnection) tokenConn.getRawConnection();
            // Create one CountDownLatch that will be reset in the callback function
            final CountDownLatch latch = new CountDownLatch(1);
            // Create an AtomicReference object to receive ClientResponse from callback function
            final AtomicReference<ClientResponse> reference = new AtomicReference<>();
            String message = "/portal/query?cmd=" + URLEncoder.encode(s, "UTF-8");
            final ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(message);
            request.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer " + RuleLoaderConfig.load().getPortalToken());
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            conn.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
            int statusCode = reference.get().getResponseCode();
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            if(statusCode != 200) {
                Status status = Config.getInstance().getMapper().readValue(body, Status.class);
                result = Failure.of(status);
            } else result = Success.of(body);
        } catch (Exception e) {
            logger.error("Exception:", e);
            Status status = new Status(GENERIC_EXCEPTION, e.getMessage());
            result = Failure.of(status);
        }
        return result;
    }

    public static Result<String> getServicePermission(String url, String hostId, String apiId, String apiVersion) {
        final String s = String.format("{\"host\":\"lightapi.net\",\"service\":\"market\",\"action\":\"getServicePermission\",\"version\":\"0.1.0\",\"data\":{\"hostId\":\"%s\",\"apiId\":\"%s\",\"apiVersion\":\"%s\"}}", hostId, apiId, apiVersion);
        Result<String> result = null;
        ClientConnection conn = null;
        try {
            SimpleConnectionHolder.ConnectionToken tokenConn = client.borrow(new URI(url), Http2Client.WORKER, client.getDefaultXnioSsl(), Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));

            conn = (ClientConnection) tokenConn.getRawConnection();
            // Create one CountDownLatch that will be reset in the callback function
            final CountDownLatch latch = new CountDownLatch(1);
            // Create an AtomicReference object to receive ClientResponse from callback function
            final AtomicReference<ClientResponse> reference = new AtomicReference<>();
            String message = "/portal/query?cmd=" + URLEncoder.encode(s, "UTF-8");
            final ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(message);
            request.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer " + RuleLoaderConfig.load().getPortalToken());
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            conn.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
            int statusCode = reference.get().getResponseCode();
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            if(statusCode != 200) {
                Status status = Config.getInstance().getMapper().readValue(body, Status.class);
                result = Failure.of(status);
            } else result = Success.of(body);
        } catch (Exception e) {
            logger.error("Exception:", e);
            Status status = new Status(GENERIC_EXCEPTION, e.getMessage());
            result = Failure.of(status);
        }
        return result;
    }

}
