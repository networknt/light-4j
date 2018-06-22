package com.networknt.handler;

import com.networknt.config.Config;
import com.networknt.handler.config.HandlerConfig;
import com.networknt.handler.config.PathChain;
import com.networknt.service.ServiceUtil;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HttpString;
import io.undertow.util.PathTemplateMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Handler {

    private static final AttachmentKey<Integer> CHAIN_SEQ = AttachmentKey.create(Integer.class);
    private static final AttachmentKey<Integer> CHAIN_ID = AttachmentKey.create(Integer.class);
    private static final Logger logger = LoggerFactory.getLogger(Handler.class);
    private static final String CONFIG_NAME = "handler";
    public static HandlerConfig config = (HandlerConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, HandlerConfig.class);

    // each handler keyed by a name.
    private static final Map<String, HttpHandler> handlers = new HashMap<>();
    private static final Map<Integer, List<HttpHandler>> handlerListById = new HashMap<>();
    private static final Map<HttpString, PathTemplateMatcher<Integer>> verbMatcherMap = new HashMap<>();

    static {
        try {
            initHandlers();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        initPaths();
    }

    public static void next(HttpServerExchange httpServerExchange) throws Exception {
        Integer chainId = httpServerExchange.getAttachment(CHAIN_ID);
        if (chainId != null) {
            List<HttpHandler> handlersForId = handlerListById.get(chainId);
            Integer nextIndex = httpServerExchange.getAttachment(CHAIN_SEQ);
            if (nextIndex < handlers.size()) {
                httpServerExchange.putAttachment(CHAIN_SEQ, nextIndex + 1);
                handlersForId.get(nextIndex).handleRequest(httpServerExchange);
            }
        } else {

        }
    }

    public static void start(HttpServerExchange httpServerExchange) {
        PathTemplateMatcher<Integer> pathTemplateMatcher = verbMatcherMap.get(httpServerExchange.getRequestMethod());
        if (pathTemplateMatcher != null) {
            PathTemplateMatcher.PathMatchResult<Integer> result = pathTemplateMatcher.match(httpServerExchange.getRequestPath());
            if (result != null) {
                Integer id = result.getValue();
                httpServerExchange.putAttachment(CHAIN_ID, id);
            } // Else no match for path
        } // Else no matcher for request type
        httpServerExchange.putAttachment(CHAIN_SEQ, 0);
    }

    private static void initPaths() {
        for (PathChain pathChain : config.getPaths()) {
            HttpString verb = new HttpString(pathChain.getVerb());
            PathTemplateMatcher<Integer> pathTemplateMatcher;
            Integer randInt = new Random().nextInt();
            List<HttpHandler> handlers = getHandlersFromExecList(pathChain.getExec());
            if (verbMatcherMap.containsKey(verb)) {
                pathTemplateMatcher = verbMatcherMap.get(verb);
                if (handlers.size() > 0) {
                    pathTemplateMatcher.add(pathChain.getPath(), randInt);
                    handlerListById.put(randInt, handlers);
                }
            } else {
                pathTemplateMatcher = new PathTemplateMatcher<>();
                if (handlers.size() > 0) {
                    pathTemplateMatcher.add(pathChain.getPath(), randInt);
                    handlerListById.put(randInt, handlers);
                }
                verbMatcherMap.put(verb, pathTemplateMatcher);
            }
        }
    }

    private static List<HttpHandler> getHandlersFromExecList(List<String> execs) {
        List<HttpHandler> handlers = new ArrayList<>();
        for (String exec : execs) {
            if (config.getChains().containsKey(exec)) {
                List<String> chainItems = config.getChains().get(exec);
                for (String chainItem : chainItems) {
                    handlers.add(Handler.handlers.get(chainItem));
                }
            } else {
                handlers.add(Handler.handlers.get(exec));
            }
        }
        return handlers;
    }

    private static void initHandlers() throws Exception {
        for (Object handler: config.getHandlers()) {
            // String handlers could be of the format: class@beanName
            if (handler instanceof String) {
                Map<String, Class> namedClass = splitClassAndName((String) handler);
                Map.Entry<String, Class> handlerSplit = namedClass.entrySet().iterator().next();
                handlers.put(handlerSplit.getKey(), (HttpHandler)handlerSplit.getValue().newInstance());
            } else if (handler instanceof Map) {
                // keys are the class name, values are the parameters.
                for (Map.Entry<String, Object> entry : ((Map<String, Object>) handler).entrySet()) {
                    Map<String, Class> namedClass = splitClassAndName(entry.getKey());
                    Map.Entry<String, Class> handlerSplit = namedClass.entrySet().iterator().next();
                    if (entry.getValue() instanceof Map) {
                        handlers.put(handlerSplit.getKey(), (HttpHandler)ServiceUtil.constructByNamedParams(handlerSplit.getValue(), (Map)entry.getValue()));
                    } else if (entry.getValue() instanceof List) {
                        handlers.put(handlerSplit.getKey(), (HttpHandler)ServiceUtil.constructByParameterizedConstructor(handlerSplit.getValue(), (List)entry.getValue()));
                    }
                }
            }
        }
    }

    private static Map<String, Class> splitClassAndName(String classLabel) throws Exception {
        String[] stringNameSplit = classLabel.split("@");
        // If i don't have a @, then no name is provided, use the class as the name.
        if (stringNameSplit.length == 1) {
            return Collections.singletonMap(classLabel, Class.forName(classLabel));
        } else if (stringNameSplit.length > 1) { // Found a @, use that as the name, and
            return Collections.singletonMap(stringNameSplit[1], Class.forName(stringNameSplit[0]));
        }
        throw new Exception("Invalid format provided for class label: "+ classLabel);
    }


    public HandlerConfig getConfig() {
        return config;
    }
}
