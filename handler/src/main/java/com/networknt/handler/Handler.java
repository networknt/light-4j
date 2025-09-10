/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.handler;

import com.networknt.config.Config;
import com.networknt.handler.config.EndpointSource;
import com.networknt.handler.config.HandlerConfig;
import com.networknt.handler.config.PathChain;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.PathTemplateMatcher;
import com.networknt.utility.Tuple;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HttpString;
import io.undertow.websockets.WebSocketConnectionCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static io.undertow.Handlers.websocket;
import static io.undertow.util.PathTemplateMatch.ATTACHMENT_KEY;

/**
 * @author Nicholas Azar
 */
public class Handler {

    private static final AttachmentKey<Integer> CHAIN_SEQ = AttachmentKey.create(Integer.class);
    private static final AttachmentKey<String> CHAIN_ID = AttachmentKey.create(String.class);
    private static final AttachmentKey<HandlerMetricsCollector> EXECUTION_METRIC = AttachmentKey.create(HandlerMetricsCollector.class);
    private static final AttachmentKey<String> METRICS_REPORT = AttachmentKey.create(String.class);
    private static final Logger LOG = LoggerFactory.getLogger(Handler.class);
    // Accessed directly.
    public static HandlerConfig config = HandlerConfig.load();

    // each handler keyed by a name.
    static final Map<String, HttpHandler> handlers = new HashMap<>();
    static final Map<String, List<HttpHandler>> handlerListById = new HashMap<>();
    static final Map<HttpString, PathTemplateMatcher<String>> methodToMatcherMap = new HashMap<>();
    static List<HttpHandler> defaultHandlers;
    // this is the last handler that need to be called when OrchestratorHandler is injected into the beginning of the chain
    static HttpHandler lastHandler;

    public static void setLastHandler(HttpHandler handler) {
        lastHandler = handler;
    }

    public static void init() {
        initHandlers();
        initChains();
        initPaths();
        initDefaultHandlers();
        ModuleRegistry.registerModule(HandlerConfig.CONFIG_NAME, Handler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(HandlerConfig.CONFIG_NAME), null);
    }

    /**
     * Construct the named map of handlers. Note: All handlers in use for this
     * microservice should be listed in this handlers list.
     */
    @SuppressWarnings("unchecked")
    static void initHandlers() {
        if (config != null && config.getHandlers() != null) {

            // initialize handlers
            for (var handler : config.getHandlers()) {
                // handler is a fully qualified class name with a default constructor.
                initStringDefinedHandler(handler);
            }
        }
    }

    /**
     * Construct chains of handlers, if any are configured NOTE: It is recommended
     * to define reusable chains of handlers
     */
    static void initChains() {

        if (config != null && config.getChains() != null) {

            // add the chains to the handler list by id list.
            for (var chainName : config.getChains().keySet()) {
                var chain = config.getChains().get(chainName);
                var handlerChain = new ArrayList<HttpHandler>();

                for (var chainItemName : chain) {
                    var chainItem = handlers.get(chainItemName);

                    if (chainItem == null)
                        throw new RuntimeException("Chain " + chainName + " uses Unknown handler: " + chainItemName);

                    handlerChain.add(chainItem);
                }
                handlerListById.put(chainName, handlerChain);
            }
        }
    }

    /**
     * Build "handlerListById" and "reqTypeMatcherMap" from the paths in the config.
     */
    static void initPaths() {

        if (config != null && config.getPaths() != null) {

            for (var pathChain : config.getPaths()) {
                pathChain.validate(HandlerConfig.CONFIG_NAME + " config"); // raises exception on misconfiguration

                if (pathChain.getPath() == null)
                    addSourceChain(pathChain);

                else addPathChain(pathChain);
            }
        }
    }

    /**
     * Build "defaultHandlers" from the defaultHandlers in the config.
     */
    static void initDefaultHandlers() {

        if (config != null && config.getDefaultHandlers() != null) {
            defaultHandlers = getHandlersFromExecList(config.getDefaultHandlers());
            handlerListById.put("defaultHandlers", defaultHandlers);
        }
    }

    /**
     * Add PathChains crated from the EndpointSource given in sourceChain
     */
    private static void addSourceChain(PathChain sourceChain) {
        try {
            var sourceClass = Class.forName(sourceChain.getSource());
            var source = (EndpointSource) (sourceClass.getDeclaredConstructor().newInstance());

            for (var endpoint : source.listEndpoints()) {
                var sourcedPath = new PathChain();
                sourcedPath.setPath(endpoint.getPath());
                sourcedPath.setMethod(endpoint.getMethod());
                sourcedPath.setExec(sourceChain.getExec());
                sourcedPath.validate(sourceChain.getSource());
                addPathChain(sourcedPath);
            }
        } catch (Exception e) {

            if (LOG.isErrorEnabled())
                LOG.error("Failed to inject handler.yml paths from: {}", sourceChain);

            if (e instanceof RuntimeException)
                throw (RuntimeException) e;

            else throw new RuntimeException(e);
        }
    }

    /**
     * Add a PathChain (having a non-null path) to the handler data structures.
     */
    private static void addPathChain(PathChain pathChain) {
        var method = new HttpString(pathChain.getMethod());

        // Use a random integer as the id for a given path.
        int randInt = new Random().nextInt();

        while (handlerListById.containsKey(Integer.toString(randInt)))
            randInt = new Random().nextInt();

        // Flatten out the execution list from a mix of middleware chains and handlers.
        var handlers = getHandlersFromExecList(pathChain.getExec());

        if (!handlers.isEmpty()) {

            // If a matcher already exists for the given type, at to that instead of
            // creating a new one.
            PathTemplateMatcher<String> pathTemplateMatcher = methodToMatcherMap.containsKey(method)
                    ? methodToMatcherMap.get(method)
                    : new PathTemplateMatcher<>();

            if (pathTemplateMatcher.get(pathChain.getPath()) == null)
                pathTemplateMatcher.add(pathChain.getPath(), Integer.toString(randInt));

            methodToMatcherMap.put(method, pathTemplateMatcher);
            handlerListById.put(Integer.toString(randInt), handlers);
        }
    }

    /**
     * Handle the next request in the chain.
     *
     * @param httpServerExchange The current requests server exchange.
     * @throws Exception Propagated exception in the handleRequest chain.
     */
    public static void next(final HttpServerExchange httpServerExchange) throws Exception {
        final var httpHandler = getNext(httpServerExchange);

        if (config.isEnabledHandlerMetrics()) {

            final var metrics = httpServerExchange.getAttachment(EXECUTION_METRIC);

            final String handlerName;
            if (httpHandler != null)
                handlerName = httpHandler.toString();

            else if (lastHandler != null)
                handlerName = lastHandler.toString();

            else handlerName = "unknown";

            metrics.initNextHandlerMeasurement(handlerName);
        }

        if (httpHandler != null)
            httpHandler.handleRequest(httpServerExchange);

        else if (lastHandler != null)
            lastHandler.handleRequest(httpServerExchange);

        if (config.isEnabledHandlerMetrics()) {
            final var metrics = httpServerExchange.getAttachment(EXECUTION_METRIC);
            final var report = metrics.finalizeHandlerMetrics();
            httpServerExchange.putAttachment(METRICS_REPORT, report);
        }
    }

    /**
     * Go to the next handler if the given next is none null. Reason for this is for
     * middleware to provide their instance next if it exists. Since if it exists,
     * the server hasn't been able to find the handler.yml.
     *
     * @param httpServerExchange The current requests server exchange.
     * @param next               The next HttpHandler to go to if it's not null.
     * @throws Exception exception
     */
    public static void next(HttpServerExchange httpServerExchange, HttpHandler next) throws Exception {

        if (next != null)
            next.handleRequest(httpServerExchange);

        else next(httpServerExchange);
    }

    /**
     * Allow nexting directly to a flow.
     *
     * @param ex The current requests server exchange.
     * @param execName           The name of the next executable to go to, ie chain or handler.
     *                           Chain resolved first.
     * @param returnToOrigFlow   True if you want to call the next handler defined in your original
     *                           chain after the provided execName is completed. False otherwise.
     * @throws Exception exception
     */
    public static void next(HttpServerExchange ex, String execName, Boolean returnToOrigFlow) throws Exception {
        var currentChainId = ex.getAttachment(CHAIN_ID);
        var currentNextIndex = ex.getAttachment(CHAIN_SEQ);

        ex.putAttachment(CHAIN_ID, execName);
        ex.putAttachment(CHAIN_SEQ, 0);

        next(ex);

        // return to current flow.
        if (returnToOrigFlow) {
            ex.putAttachment(CHAIN_ID, currentChainId);
            ex.putAttachment(CHAIN_SEQ, currentNextIndex);
            next(ex);
        }
    }

    /**
     * Returns the instance of the next handler, rather then calling handleRequest
     * on it.
     *
     * @param httpServerExchange The current requests server exchange.
     * @return The HttpHandler that should be executed next.
     */
    public static HttpHandler getNext(HttpServerExchange httpServerExchange) {
        var chainId = httpServerExchange.getAttachment(CHAIN_ID);
        var handlersForId = handlerListById.get(chainId);
        var nextIndex = httpServerExchange.getAttachment(CHAIN_SEQ);

        // Check if we've reached the end of the chain.
        if (nextIndex < handlersForId.size()) {
            httpServerExchange.putAttachment(CHAIN_SEQ, nextIndex + 1);
            return handlersForId.get(nextIndex);
        }

        return null;
    }

    /**
     * Returns the instance of the next handler, or the given next param if it's not
     * null.
     *
     * @param httpServerExchange The current requests server exchange.
     * @param next               If not null, return this.
     * @return The next handler in the chain, or next if it's not null.
     * @throws Exception exception
     */
    public static HttpHandler getNext(HttpServerExchange httpServerExchange, HttpHandler next) throws Exception {
        if (next != null)
            return next;

        return getNext(httpServerExchange);
    }

    /**
     * On the first step of the request, match the request against the configured
     * paths. If the match is successful, store the chain id within the exchange.
     * Otherwise return false.
     *
     * @param ex The current requests server exchange.
     * @return true if a handler has been defined for the given path.
     */
    public static boolean start(HttpServerExchange ex) {

        // Get the matcher corresponding to the current request type.
        var pathTemplateMatcher = methodToMatcherMap.get(ex.getRequestMethod());

        if (pathTemplateMatcher != null) {

            // Match the current request path to the configured paths.
            var result = pathTemplateMatcher.match(ex.getRequestPath());

            if (result != null) {

                if (config.isEnabledHandlerMetrics()) {
                    ex.putAttachment(EXECUTION_METRIC, new HandlerMetricsCollector());
                }

                // Found a match, configure and return true;
                // Add path variables to query params.
                ex.putAttachment(ATTACHMENT_KEY, new io.undertow.util.PathTemplateMatch(result.getMatchedTemplate(), result.getParameters()));

                for (var entry : result.getParameters().entrySet()) {

                    // the values shouldn't be added to query param. but this is left as it was to keep backward compatability
                    ex.addQueryParam(entry.getKey(), entry.getValue());

                    // put values in path param map
                    ex.addPathParam(entry.getKey(), entry.getValue());
                }

                var id = result.getValue();
                ex.putAttachment(CHAIN_ID, id);
                ex.putAttachment(CHAIN_SEQ, 0);
                return true;
            }
        }
        return false;
    }


    /**
     * If there is no matching path, the OrchestrationHandler is going to try to start the defaultHandlers.
     * If there are default handlers defined, store the chain id within the exchange.
     * Otherwise, return false.
     *
     * @param ex The current requests server exchange.
     * @return true if a handler has been defined for the given path.
     */
    public static boolean startDefaultHandlers(HttpServerExchange ex) {

        // check if defaultHandlers is empty
        if (defaultHandlers != null && defaultHandlers.size() > 0) {
            ex.putAttachment(CHAIN_ID, "defaultHandlers");
            ex.putAttachment(CHAIN_SEQ, 0);
            return true;
        }
        return false;
    }

    /**
     * Converts the list of chains and handlers to a flat list of handlers. If a
     * chain is named the same as a handler, the chain is resolved first.
     *
     * @param execs The list of names of chains and handlers.
     * @return A list containing references to the instantiated handlers
     */
    private static List<HttpHandler> getHandlersFromExecList(List<String> execs) {
        var handlersFromExecList = new ArrayList<HttpHandler>();

        if (execs != null) {

            for (var exec : execs) {
                var handlerList = handlerListById.get(exec);

                if (handlerList == null)
                    throw new RuntimeException("Unknown handler or chain: " + exec);

                for (HttpHandler handler : handlerList) {

                    if (handler instanceof MiddlewareHandler) {

                        if (((MiddlewareHandler) handler).isEnabled())
                            handlersFromExecList.add(handler);

                    } else handlersFromExecList.add(handler);
                }
            }
        }
        return handlersFromExecList;
    }

    /**
     * Detect if the handler is a MiddlewareHandler instance. If yes, then register it.
     *
     * @param handler
     */
    private static void registerMiddlewareHandler(Object handler) {

        if (handler instanceof MiddlewareHandler) {

            // register the middleware handler if it is enabled.
            if (((MiddlewareHandler) handler).isEnabled())
                ((MiddlewareHandler) handler).register();
        }
    }

    /**
     * Helper method for generating the instance of a handler from its string
     * definition in config. Ie. No mapped values for setters, or list of
     * constructor fields. To note: It could either implement HttpHandler, or
     * HandlerProvider.
     *
     * @param handler handler string
     */
    private static void initStringDefinedHandler(String handler) {

        // split the class name and its label, if defined
        Tuple<String, Class> namedClass = splitClassAndName(handler);

        // create an instance of the handler
        Object handlerOrProviderObject = null;
        try {
            handlerOrProviderObject = namedClass.second.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            LOG.error("Could not instantiate handler class " + namedClass.second, e);
            e.printStackTrace();
            throw new RuntimeException("Could not instantiate handler class: " + namedClass.second);
        }

        HttpHandler resolvedHandler;

        if (handlerOrProviderObject instanceof HttpHandler)
            resolvedHandler = (HttpHandler) handlerOrProviderObject;

        else if (handlerOrProviderObject instanceof HandlerProvider)
            resolvedHandler = ((HandlerProvider) handlerOrProviderObject).getHandler();

        else if (handlerOrProviderObject instanceof WebSocketConnectionCallback)
            resolvedHandler = websocket((WebSocketConnectionCallback) handlerOrProviderObject);

        else throw new RuntimeException("Unsupported type of handler provided: " + handlerOrProviderObject);

        registerMiddlewareHandler(resolvedHandler);
        handlers.put(namedClass.first, resolvedHandler);
        handlerListById.put(namedClass.first, Collections.singletonList(resolvedHandler));
    }

    /*
     * Helper method for generating the instance of a handler from its map
     * definition in config. Ie. No mapped values for setters, or list of
     * constructor fields.
     *
     * @param handler handler map
     * As all handlers have a default constructor with a configuration file, there is no need to pass any parameters.
    private static void initMapDefinedHandler(Map<String, Object> handler) {
        // If the handler is a map, the keys are the class name, values are the
        // parameters.
        for (Map.Entry<String, Object> entry : handler.entrySet()) {
            Tuple<String, Class> namedClass = splitClassAndName(entry.getKey());

            // If the values in the config are a map, construct the object using named
            // parameters.
            if (entry.getValue() instanceof Map) {
                HttpHandler httpHandler;
                try {
                    httpHandler = (HttpHandler) ServiceUtil.constructByNamedParams(namedClass.second,
                            (Map) entry.getValue());
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Could not construct a handler with values provided as a map: " + namedClass.second);
                }
                registerMiddlewareHandler(httpHandler);
                handlers.put(namedClass.first, httpHandler);
                handlerListById.put(namedClass.first, Collections.singletonList(httpHandler));
            } else if (entry.getValue() instanceof List) {

                // If the values in the config are a list, call the constructor of the handler
                // with those fields.
                HttpHandler httpHandler;
                try {
                    httpHandler = (HttpHandler) ServiceUtil.constructByParameterizedConstructor(namedClass.second,
                            (List) entry.getValue());
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Could not construct a handler with values provided as a list: " + namedClass.second);
                }
                registerMiddlewareHandler(httpHandler);
                handlers.put(namedClass.first, httpHandler);
                handlerListById.put(namedClass.first, Collections.singletonList(httpHandler));
            }
        }
    }
    */

    /**
     * To support multiple instances of the same class, support a naming
     *
     * @param classLabel The label as seen in the config file.
     * @return A tuple where the first value is the name, and the second is the
     * class.
     * @throws Exception On invalid format of label.
     */
    static Tuple<String, Class> splitClassAndName(String classLabel) {
        String[] stringNameSplit = classLabel.split("@");
        // If i don't have a @, then no name is provided, use the class as the name.
        if (stringNameSplit.length == 1) {
            try {
                return new Tuple<>(classLabel, Class.forName(classLabel));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Configured class: " + classLabel + " has not been found");
            }
        } else if (stringNameSplit.length > 1) { // Found a @, use that as the name, and
            try {
                return new Tuple<>(stringNameSplit[1], Class.forName(stringNameSplit[0]));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Configured class: " + stringNameSplit[0]
                        + " has not been found. Declared label was: " + stringNameSplit[1]);
            }
        }
        throw new RuntimeException("Invalid format provided for class label: " + classLabel);
    }

    // Exposed for testing only.
    static void setConfig(String configName) throws Exception {
        config = HandlerConfig.load(configName);
        initHandlers();
        initChains();
        initPaths();
    }

    public static Map<String, HttpHandler> getHandlers() {
        return handlers;
    }

    protected static class HandlerMetricsCollector {
        private boolean completed = false;
        private final ArrayList<StopWatch> metrics = new ArrayList<>();

        public void initNextHandlerMeasurement(final String handlerName) {
            this.stopPreviousHandler();
            final var nextHandler = new StopWatch(handlerName);
            nextHandler.start();
            this.metrics.add(nextHandler);
        }

        private void stopPreviousHandler() {
            if (!metrics.isEmpty()) {
                final var previousHandler = this.metrics.get(metrics.size() - 1);
                previousHandler.stop();
            }
        }

        public String finalizeHandlerMetrics() {

            if (this.completed)
                throw new IllegalStateException("Metrics already finalized.");

            this.completed = true;
            this.stopPreviousHandler();
            final var report = this.buildMetricsReport();
            LOG.atLevel(Level.valueOf(config.getHandlerMetricsLogLevel())).log(report);
            return report;
        }

        private String buildMetricsReport() {
            final var metricsDisplay = new StringBuilder();
            metricsDisplay.append("[");
            for (int x = 1; !this.metrics.isEmpty(); x++) {
                final var currentHandler = this.metrics.remove(0);

                if (currentHandler.isRunning())
                    throw new IllegalStateException("Handler metric stop watch is still running!");
                metricsDisplay.append("{").append("\"num\": ").append(x).append(", ");
                metricsDisplay.append("\"name\": ").append("\"").append(currentHandler.getName()).append("\", ");
                metricsDisplay.append("\"duration\": ").append(currentHandler.getDuration()).append("}");

                if (!this.metrics.isEmpty()) {
                    metricsDisplay.append(", ");
                }
            }
            metricsDisplay.append("]");
            return metricsDisplay.toString();
        }
    }

    private static class StopWatch {
        private long startTime;
        private long endTime;
        private boolean running = false;
        private final String name;

        public StopWatch(final String name) {
            this.name = name;
        }

        public boolean isRunning() {
            return this.running;
        }

        public void start() {
            if (this.running)
                throw new IllegalStateException("Cannot start twice!");

            this.startTime = System.currentTimeMillis();
            this.running = true;
        }

        public void stop() {
            if (!this.running)
                throw new IllegalStateException("Cannot end twice!");

            this.running = false;

            this.endTime = System.currentTimeMillis();
        }

        public String getName() {
            return this.name;
        }

        public long getDuration() {
            if (this.running)
                throw new IllegalStateException("Watch must be stopped first.");

            return this.endTime - this.startTime;
        }

    }
}
