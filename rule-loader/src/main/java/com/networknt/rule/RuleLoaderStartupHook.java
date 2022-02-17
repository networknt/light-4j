package com.networknt.rule;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.monad.Success;
import com.networknt.server.ServerConfig;
import com.networknt.server.StartupHookProvider;
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

import java.net.URI;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;


/**
 * This is the startup hook to load YAML rules from the light-portal during the server startup. Currently,
 * only the access-control is using the rule engine; however, we are expecting more components will follow.
 *
 * @author Steve Hu
 */
public class RuleLoaderStartupHook implements StartupHookProvider {
    static final Logger logger = LoggerFactory.getLogger(RuleLoaderStartupHook.class);
    // shared rule map with ruleId as the key and Rule object as the value.
    public static Map<String, Object> endpointRules;
    public static Map<String, Rule> rules;
    private static RuleLoaderConfig config = (RuleLoaderConfig) Config.getInstance().getJsonObjectConfig(RuleLoaderConfig.CONFIG_NAME, RuleLoaderConfig.class);
    static Http2Client client = Http2Client.getInstance();
    static final String GENERIC_EXCEPTION = "ERR10014";
    static final String DEFAULT_HOST = "lightapi.net";

    @Override
    public void onStartup() {
        ServerConfig serverConfig = (ServerConfig)Config.getInstance().getJsonObjectConfig(ServerConfig.CONFIG_NAME, ServerConfig.class);
        Result<String> result = getServiceById(config.getPortalHost(), serverConfig.getServiceId());
        if(result.isSuccess()) {
            String serviceString = result.getResult();
            if(logger.isDebugEnabled()) logger.debug("getServiceById result = " + serviceString);
            Map<String, Object> objectMap = JsonMapper.string2Map(serviceString);
            endpointRules = (Map<String, Object>)objectMap.get("endpointRules");
            // need to get the rule bodies here to create a map of ruleId to ruleBody.
            Iterator<Object> iterator = endpointRules.values().iterator();
            String ruleString = "\n";
            while (iterator.hasNext()) {
                Map<String, List> value = (Map<String, List>)iterator.next();
                Iterator<List> iteratorList = value.values().iterator();
                while(iteratorList.hasNext()) {
                    List<Map<String, String>> list = iteratorList.next();
                    for(Map<String, String> map: list) {
                        Iterator<String> iteratorRule = map.values().iterator();
                        while(iteratorRule.hasNext()) {
                            String ruleId = iteratorRule.next();
                            if(logger.isDebugEnabled()) logger.debug("Load rule for ruleId = " + ruleId);
                            // get rule content for each id and concat them together.
                            String r = getRuleById(config.getPortalHost(), DEFAULT_HOST, ruleId).getResult();
                            Map<String, Object> ruleMap = JsonMapper.string2Map(r);
                            ruleString = ruleString + ruleMap.get("value") + "\n";
                        }
                    }
                }
            }
            System.out.println(ruleString);
            rules = RuleMapper.string2RuleMap(ruleString);
            if(logger.isInfoEnabled()) logger.info("Load YAML rules with size = " + rules.size());
        } else {
            logger.error("Could not load rule for serviceId = " + serverConfig.getServiceId());
        }
    }

    public static Result<String> getServiceById(String url, String serviceId) {
        final String s = String.format("{\"host\":\"lightapi.net\",\"service\":\"market\",\"action\":\"getServiceById\",\"version\":\"0.1.0\",\"data\":{\"serviceId\":\"%s\"}}", serviceId);
        Result<String> result = null;
        ClientConnection conn = null;
        try {
            conn = client.connect(new URI(url), Http2Client.WORKER, client.getDefaultXnioSsl(), Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
            // Create one CountDownLatch that will be reset in the callback function
            final CountDownLatch latch = new CountDownLatch(1);
            // Create an AtomicReference object to receive ClientResponse from callback function
            final AtomicReference<ClientResponse> reference = new AtomicReference<>();
            String message = "/portal/query?cmd=" + URLEncoder.encode(s, "UTF-8");
            final ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(message);
            request.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer " + config.getPortalToken());
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


    public static Result<String> getRuleById(String url, String host, String ruleId) {
        final String s = String.format("{\"host\":\"lightapi.net\",\"service\":\"market\",\"action\":\"getRuleById\",\"version\":\"0.1.0\",\"data\":{\"host\":\"%s\",\"ruleId\":\"%s\"}}", host, ruleId);
        Result<String> result = null;
        ClientConnection conn = null;
        try {
            conn = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
            // Create one CountDownLatch that will be reset in the callback function
            final CountDownLatch latch = new CountDownLatch(1);
            // Create an AtomicReference object to receive ClientResponse from callback function
            final AtomicReference<ClientResponse> reference = new AtomicReference<>();
            String message = "/portal/query?cmd=" + URLEncoder.encode(s, "UTF-8");
            final ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(message);
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
