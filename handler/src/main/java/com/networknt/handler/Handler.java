package com.networknt.handler;

import com.networknt.common.Tuple;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author Nicholas Azar
 */
public class Handler {

    private static final AttachmentKey<Integer> CHAIN_SEQ = AttachmentKey.create(Integer.class);
    private static final AttachmentKey<Integer> CHAIN_ID = AttachmentKey.create(Integer.class);
    private static final Logger logger = LoggerFactory.getLogger(Handler.class);
    private static final String CONFIG_NAME = "handler";
    public static HandlerConfig config = (HandlerConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, HandlerConfig.class);

    // each handler keyed by a name.
    private static final Map<String, HttpHandler> handlers = new HashMap<>();
    private static final Map<Integer, List<HttpHandler>> handlerListById = new HashMap<>();
    private static final Map<HttpString, PathTemplateMatcher<Integer>> reqTypeMatcherMap = new HashMap<>();

    static {
        try {
            initHandlers();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        initPaths();
    }

    /**
     * Handle the next request in the chain.
     *
     * @param httpServerExchange The current requests server exchange.
     * @throws Exception Propagated exception in the handleRequest chain.
     */
    public static void next(HttpServerExchange httpServerExchange) throws Exception {
        HttpHandler httpHandler = getNext(httpServerExchange);
        if (httpHandler != null) {
            httpHandler.handleRequest(httpServerExchange);
        }
    }

    public static void next(HttpServerExchange httpServerExchange, HttpHandler next) throws Exception {
        if (next != null) {
            next.handleRequest(httpServerExchange);
        } else {
            next(httpServerExchange);
        }
    }

    public static HttpHandler getNext(HttpServerExchange httpServerExchange) {
        Integer chainId = httpServerExchange.getAttachment(CHAIN_ID);
        List<HttpHandler> handlersForId = handlerListById.get(chainId);
        Integer nextIndex = httpServerExchange.getAttachment(CHAIN_SEQ);
        // Check if we've reached the end of the chain.
        if (nextIndex < handlersForId.size()) {
            httpServerExchange.putAttachment(CHAIN_SEQ, nextIndex + 1);
            return handlersForId.get(nextIndex);
        }
        return null;
    }

    public static HttpHandler getNext(HttpServerExchange httpServerExchange, HttpHandler next) throws Exception {
        if (next != null) {
            return next;
        }
        return getNext(httpServerExchange);
    }

    /**
     * On the first step of the request, match the request against the configured paths. If the match is successful,
     * store the chain id within the exchange. Otherwise return false.
     *
     * @param httpServerExchange The current requests server exchange.
     * @return true if a handler has been defined for the given path.
     */
    public static boolean start(HttpServerExchange httpServerExchange) {
        // Get the matcher corresponding to the current request type.
        PathTemplateMatcher<Integer> pathTemplateMatcher = reqTypeMatcherMap.get(httpServerExchange.getRequestMethod());
        if (pathTemplateMatcher != null) {
            // Match the current request path to the configured paths.
            PathTemplateMatcher.PathMatchResult<Integer> result = pathTemplateMatcher.match(httpServerExchange.getRequestPath());
            if (result != null) {
                // Found a match, configure and return true;
                Integer id = result.getValue();
                httpServerExchange.putAttachment(CHAIN_ID, id);
                httpServerExchange.putAttachment(CHAIN_SEQ, 0);
                return true;
            }
        }
        return false;
    }

    /**
     * Build "handlerListById" and "reqTypeMatcherMap" from the paths in the config.
     */
    private static void initPaths() {
        if (config != null && config.getPaths() != null) {
            for (PathChain pathChain : config.getPaths()) {
                HttpString requestType = new HttpString(pathChain.getRequestType());

                // Use a random integer as the id for a given path.
                Integer randInt = new Random().nextInt();
                // Flatten out the execution list from a mix of middleware chains and handlers.
                List<HttpHandler> handlers = getHandlersFromExecList(pathChain.getExec());
                if (handlers.size() > 0) {
                    // If a matcher already exists for the given type, at to that instead of creating a new one.
                    PathTemplateMatcher<Integer> pathTemplateMatcher = reqTypeMatcherMap.containsKey(requestType) ? reqTypeMatcherMap.get(requestType) : new PathTemplateMatcher<>();
                    pathTemplateMatcher.add(pathChain.getPath(), randInt);
                    reqTypeMatcherMap.put(requestType, pathTemplateMatcher);
                    handlerListById.put(randInt, handlers);
                }
            }
        }
    }

    /**
     * Converts the list of chains and handlers to a flat list of handlers.
     * If a chain is named the same as a handler, the chain is resolved first.
     *
     * @param execs The list of names of chains and handlers.
     * @return A list containing references to the instantiated handlers
     */
    private static List<HttpHandler> getHandlersFromExecList(List<String> execs) {
        List<HttpHandler> handlersFromExecList = new ArrayList<>();
        if (execs != null) {
            for (String exec : execs) {
                // If the given exec is a chain, resolve the chain into handlers.
                List<String> chainItems = config.getChains().get(exec);
                if (chainItems != null) {
                    chainItems.forEach(chainItem -> handlersFromExecList.add(handlers.get(chainItem)));
                } else {
                    handlersFromExecList.add(Handler.handlers.get(exec));
                }
            }
        }
        return handlersFromExecList;
    }

    /**
     * Construct the named map of handlers.
     * @throws Exception
     */
    private static void initHandlers() throws Exception {
        if (config != null && config.getHandlers() != null) {
            for (Object handler : config.getHandlers()) {
                // If the handler is configured as just a string, it's a fully qualified class name with a default constructor.
                if (handler instanceof String) {
                    Tuple<String, Class> namedClass = splitClassAndName((String) handler);
                    handlers.put(namedClass.first, (HttpHandler) namedClass.second.newInstance());
                } else if (handler instanceof Map) {
                    // If the handler is a map, the keys are the class name, values are the parameters.
                    for (Map.Entry<String, Object> entry : ((Map<String, Object>) handler).entrySet()) {
                        Tuple<String, Class> namedClass = splitClassAndName(entry.getKey());
                        // If the values in the config are a map, construct the object using named parameters.
                        if (entry.getValue() instanceof Map) {
                            handlers.put(namedClass.first, (HttpHandler) ServiceUtil.constructByNamedParams(namedClass.second, (Map) entry.getValue()));
                        } else if (entry.getValue() instanceof List) {
                            // If the values in the config are a list, call the constructor of the handler with those fields.
                            handlers.put(namedClass.first, (HttpHandler) ServiceUtil.constructByParameterizedConstructor(namedClass.second, (List) entry.getValue()));
                        }
                    }
                }
            }
        }
    }

    /**
     * To support multiple instances of the same class, support a naming
     * @param classLabel The label as seen in the config file.
     * @return A tuple where the first value is the name, and the second is the class.
     * @throws Exception On invalid format of label.
     */
    protected static Tuple<String, Class> splitClassAndName(String classLabel) throws Exception {
        String[] stringNameSplit = classLabel.split("@");
        // If i don't have a @, then no name is provided, use the class as the name.
        if (stringNameSplit.length == 1) {
            return new Tuple<>(classLabel, Class.forName(classLabel));
        } else if (stringNameSplit.length > 1) { // Found a @, use that as the name, and
            return new Tuple<>(stringNameSplit[1], Class.forName(stringNameSplit[0]));
        }
        throw new Exception("Invalid format provided for class label: "+ classLabel);
    }

    // Exposed for testing only.
    protected void setConfig(String configName) {
        config = (HandlerConfig) Config.getInstance().getJsonObjectConfig(configName, HandlerConfig.class);
    }
}
