package com.networknt.handler;

import com.networknt.utility.Tuple;
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

import static io.undertow.util.PathTemplateMatch.ATTACHMENT_KEY;

/**
 * @author Nicholas Azar
 */
public class Handler {

    private static final AttachmentKey<Integer> CHAIN_SEQ = AttachmentKey.create(Integer.class);
    private static final AttachmentKey<String> CHAIN_ID = AttachmentKey.create(String.class);
    private static final Logger logger = LoggerFactory.getLogger(Handler.class);
    private static final String CONFIG_NAME = "handler";
    // Accessed directly.
    public
    static HandlerConfig config = (HandlerConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, HandlerConfig.class);

    // each handler keyed by a name.
    static final Map<String, HttpHandler> handlers = new HashMap<>();
    static final Map<String, List<HttpHandler>> handlerListById = new HashMap<>();
    static final Map<HttpString, PathTemplateMatcher<String>> methodToMatcherMap = new HashMap<>();

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

    /**
     * Go to the next handler if the given next is none null.
     * Reason for this is for middleware to provide their instance next if it exists. Since if it exists,
     * the server hasn't been able to find the handler.yml.
     *
     * @param httpServerExchange The current requests server exchange.
     * @param next The next HttpHandler to go to if it's not null.
     * @throws Exception
     */
    public static void next(HttpServerExchange httpServerExchange, HttpHandler next) throws Exception {
        if (next != null) {
            next.handleRequest(httpServerExchange);
        } else {
            next(httpServerExchange);
        }
    }

    /**
     * Allow nexting directly to a flow.
     *
     * @param httpServerExchange The current requests server exchange.
     * @param execName The name of the next executable to go to, ie chain or handler. Chain resolved first.
     * @param returnToOrigFlow True if you want to call the next handler defined in your original chain after the provided execName is completed. False otherwise.
     * @throws Exception
     */
    public static void next(HttpServerExchange httpServerExchange, String execName, Boolean returnToOrigFlow) throws Exception {
        String currentChainId = httpServerExchange.getAttachment(CHAIN_ID);
        Integer currentNextIndex = httpServerExchange.getAttachment(CHAIN_SEQ);

        httpServerExchange.putAttachment(CHAIN_ID, execName);
        httpServerExchange.putAttachment(CHAIN_SEQ, 0);

        next(httpServerExchange);

        // return to current flow.
        if (returnToOrigFlow) {
            httpServerExchange.putAttachment(CHAIN_ID, currentChainId);
            httpServerExchange.putAttachment(CHAIN_SEQ, currentNextIndex);
            next(httpServerExchange);
        }
    }

    /**
     * Returns the instance of the next handler, rather then calling handleRequest on it.
     *
     * @param httpServerExchange The current requests server exchange.
     * @return The HttpHandler that should be executed next.
     */
    public static HttpHandler getNext(HttpServerExchange httpServerExchange) {
        String chainId = httpServerExchange.getAttachment(CHAIN_ID);
        List<HttpHandler> handlersForId = handlerListById.get(chainId);
        Integer nextIndex = httpServerExchange.getAttachment(CHAIN_SEQ);
        // Check if we've reached the end of the chain.
        if (nextIndex < handlersForId.size()) {
            httpServerExchange.putAttachment(CHAIN_SEQ, nextIndex + 1);
            return handlersForId.get(nextIndex);
        }
        return null;
    }

    /**
     * Returns the instance of the next handler, or the given next param if it's not null.
     *
     * @param httpServerExchange The current requests server exchange.
     * @param next If not null, return this.
     * @return The next handler in the chain, or next if it's not null.
     * @throws Exception
     */
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
        PathTemplateMatcher<String> pathTemplateMatcher = methodToMatcherMap.get(httpServerExchange.getRequestMethod());
        if (pathTemplateMatcher != null) {
            // Match the current request path to the configured paths.
            PathTemplateMatcher.PathMatchResult<String> result = pathTemplateMatcher.match(httpServerExchange.getRequestPath());
            if (result != null) {
                // Found a match, configure and return true;
                // Add path variables to query params.
                httpServerExchange.putAttachment(ATTACHMENT_KEY, new io.undertow.util.PathTemplateMatch(result.getMatchedTemplate(), result.getParameters()));
                for (Map.Entry<String, String> entry : result.getParameters().entrySet()) {
                    httpServerExchange.addQueryParam(entry.getKey(), entry.getValue());
                }
                String id = result.getValue();
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
    static void initPaths() {
        if (config != null && config.getPaths() != null) {
            for (PathChain pathChain : config.getPaths()) {
                HttpString method = new HttpString(pathChain.getMethod());

                // Use a random integer as the id for a given path.
                Integer randInt = new Random().nextInt();
                while (handlerListById.containsKey(randInt.toString())) {
                    randInt = new Random().nextInt();
                }

                // Flatten out the execution list from a mix of middleware chains and handlers.
                List<HttpHandler> handlers = getHandlersFromExecList(pathChain.getExec());
                if (handlers.size() > 0) {
                    // If a matcher already exists for the given type, at to that instead of creating a new one.
                    PathTemplateMatcher<String> pathTemplateMatcher = methodToMatcherMap.containsKey(method) ? methodToMatcherMap.get(method) : new PathTemplateMatcher<>();
                    pathTemplateMatcher.add(pathChain.getPath(), randInt.toString());
                    methodToMatcherMap.put(method, pathTemplateMatcher);
                    handlerListById.put(randInt.toString(), handlers);
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
                List<HttpHandler> handerList = handlerListById.get(exec);
                if(handerList == null) throw new RuntimeException("Unknown handler or chain: " + exec);
                handlersFromExecList.addAll(handerList);
            }
        }
        return handlersFromExecList;
    }

    /**
     * Construct the named map of handlers.
     * @throws Exception
     */
    static void initHandlers() throws Exception {
        if (config != null && config.getHandlers() != null) {
            for (Object handler : config.getHandlers()) {
                // If the handler is configured as just a string, it's a fully qualified class name with a default constructor.
                if (handler instanceof String) {
                    Tuple<String, Class> namedClass = splitClassAndName((String) handler);
                    HttpHandler httpHandler = (HttpHandler) namedClass.second.newInstance();
                    handlers.put(namedClass.first, httpHandler);
                    handlerListById.put(namedClass.first, Collections.singletonList(httpHandler));
                } else if (handler instanceof Map) {
                    // If the handler is a map, the keys are the class name, values are the parameters.
                    for (Map.Entry<String, Object> entry : ((Map<String, Object>) handler).entrySet()) {
                        Tuple<String, Class> namedClass = splitClassAndName(entry.getKey());
                        // If the values in the config are a map, construct the object using named parameters.
                        if (entry.getValue() instanceof Map) {
                            HttpHandler httpHandler = (HttpHandler) ServiceUtil.constructByNamedParams(namedClass.second, (Map) entry.getValue());
                            handlers.put(namedClass.first, httpHandler);
                            handlerListById.put(namedClass.first, Collections.singletonList(httpHandler));
                        } else if (entry.getValue() instanceof List) {
                            // If the values in the config are a list, call the constructor of the handler with those fields.
                            HttpHandler httpHandler = (HttpHandler) ServiceUtil.constructByParameterizedConstructor(namedClass.second, (List) entry.getValue());
                            handlers.put(namedClass.first, httpHandler);
                            handlerListById.put(namedClass.first, Collections.singletonList(httpHandler));
                        }
                    }
                }
            }
            // Build add the chains to the handler list by id list.
            for (String chainName : config.getChains().keySet()) {
                List<String> chain = config.getChains().get(chainName);
                List<HttpHandler> handlerChain = new ArrayList<>();
                for (String chainItemName : chain) {
                    HttpHandler chainItem = handlers.get(chainItemName);
                    if(chainItem == null) throw new RuntimeException("Chain " + chainName + " uses Unknown handler: " + chainItemName);
                    handlerChain.add(chainItem);
                }
                handlerListById.put(chainName, handlerChain);
            }
        }
    }

    /**
     * To support multiple instances of the same class, support a naming
     * @param classLabel The label as seen in the config file.
     * @return A tuple where the first value is the name, and the second is the class.
     * @throws Exception On invalid format of label.
     */
    static Tuple<String, Class> splitClassAndName(String classLabel) throws Exception {
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
    static void setConfig(String configName) throws Exception {
        config = (HandlerConfig) Config.getInstance().getJsonObjectConfig(configName, HandlerConfig.class);
        initHandlers();
        initPaths();
    }
}
