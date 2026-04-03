package com.networknt.portal.registry.client;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import com.networknt.config.Config;
import com.networknt.info.ServerInfoConfig;
import com.networknt.info.ServerInfoUtil;
import com.networknt.logging.model.LoggerInfo;
import com.networknt.server.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class McpHandler {
    private static final Logger logger = LoggerFactory.getLogger(McpHandler.class);
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ";
    private static final String OBJECT = "object";
    private static final String PROPERTIES = "properties";
    private static final String INPUT_SCHEMA = "inputSchema";
    private static final String DESCRIPTION = "description";
    private static final String REQUIRED = "required";
    private static final String LOGGERS = "loggers";
    private static final String STRING = "string";
    private static final String START_TIME = "startTime";
    private static final String END_TIME = "endTime";
    private static final String LOGGER_LEVEL = "loggerLevel";
    private static final String LEVEL = "level";
    private static final String NAME = "name";
    private static final String MODULES = "modules";
    private static final String ASSAULT_TYPE = "assaultType";
    private static final String CONFIG_ARG = "config";
    private static final String STATUS = "status";
    private static final String SUCCESS = "success";
    private static final String MESSAGE = "message";
    private static final String SUPPORTED = "supported";
    private static final String TYPE = "type";
    private static final String SIZE = "size";
    private static final String KEYS = "keys";
    private static final String TOTAL = "total";
    private static final String LOGS = "logs";
    private static final String CACHES = "caches";
    private static final String ENTRIES = "entries";
    private static final String EXCEPTION = "exception";
    private static final String KILLAPP = "killapp";
    private static final String LATENCY = "latency";
    private static final String MEMORY = "memory";
    private static final Map<PortalRegistryWebSocketClient, McpLogAppender> activeLogAppenders = new IdentityHashMap<>();
    private static final String EXCEPTION_ASSAULT_HANDLER = "com.networknt.chaos.ExceptionAssaultHandler";
    private static final String KILLAPP_ASSAULT_HANDLER = "com.networknt.chaos.KillappAssaultHandler";
    private static final String LATENCY_ASSAULT_HANDLER = "com.networknt.chaos.LatencyAssaultHandler";
    private static final String MEMORY_ASSAULT_HANDLER = "com.networknt.chaos.MemoryAssaultHandler";
    private static final String CHAOS_MONKEY_UNAVAILABLE_MESSAGE =
            "Chaos monkey is not available on this service. Add the light-chaos-monkey dependencies to enable it.";
    private static final String CACHE_UNAVAILABLE_MESSAGE =
            "Cache support is not available on this service. Add the cache-manager dependencies to enable it.";

    private McpHandler() {
    }

    public static void handle(PortalRegistryWebSocketClient client, Map<String, Object> envelope) {
        Object methodObject = envelope.get("method");
        Object id = envelope.get("id");
        if (id == null) return;
        if (!(methodObject instanceof String method)) {
            client.sendError(id, -32600, "Missing or invalid method");
            return;
        }

        try {
            switch (method) {
                case "tools/list":
                    handleToolsList(client, id);
                    break;
                case "tools/call":
                    Object paramsObj = envelope.get("params");
                    if (!(paramsObj instanceof Map<?, ?> rawParams)) {
                        client.sendError(id, -32602, "Missing or invalid params for tools/call");
                        break;
                    }
                    Map<String, Object> params = new HashMap<>();
                    for (Map.Entry<?, ?> entry : rawParams.entrySet()) {
                        params.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                    handleToolsCall(client, id, params);
                    break;
                default:
                    client.sendError(id, -32601, "Method not found: " + method);
            }
        } catch (IllegalArgumentException e) {
            client.sendError(id, -32602, e.getMessage());
        } catch (Exception e) {
            logger.error("Error handling MCP request", e);
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "Internal error: " + e.getClass().getSimpleName();
            }
            client.sendError(id, -32603, errorMessage);
        }
    }

    private static void handleToolsList(PortalRegistryWebSocketClient client, Object id) {
        List<Map<String, Object>> tools = List.of(
                Map.of(
                        "name", "get_service_info",
                        DESCRIPTION, "Retrieve information about this microservice instance",
                        INPUT_SCHEMA, Map.of("type", OBJECT, PROPERTIES, Map.of())
                ),
                Map.of(
                        "name", "check",
                        DESCRIPTION, "Perform a local health check",
                        INPUT_SCHEMA, Map.of("type", OBJECT, PROPERTIES, Map.of())
                ),
                Map.of(
                        "name", "get_loggers",
                        DESCRIPTION, "Retrieve current logger configuration",
                        INPUT_SCHEMA, Map.of("type", OBJECT, PROPERTIES, Map.of())
                ),
                Map.of(
                        "name", "set_loggers",
                        DESCRIPTION, "Update logger levels",
                        INPUT_SCHEMA, Map.of(
                                "type", OBJECT,
                                PROPERTIES, Map.of(LOGGERS, Map.of("type", "array", "items", Map.of("type", OBJECT))),
                                REQUIRED, List.of(LOGGERS)
                        )
                ),
                Map.of(
                        "name", "get_log_content",
                        DESCRIPTION, "Retrieve historical log content",
                        INPUT_SCHEMA, Map.of(
                                "type", OBJECT,
                                PROPERTIES, Map.of(
                                        START_TIME, Map.of("type", "integer", DESCRIPTION, "Start time in epoch milliseconds"),
                                        END_TIME, Map.of("type", "integer", DESCRIPTION, "Optional end time in epoch milliseconds"),
                                        LOGGER_LEVEL, Map.of("type", STRING)
                                ),
                                REQUIRED, List.of(START_TIME)
                        )
                ),
                Map.of(
                        "name", "start_logs",
                        DESCRIPTION, "Start live log streaming",
                        INPUT_SCHEMA, Map.of(
                                "type", OBJECT,
                                PROPERTIES, Map.of(
                                        LEVEL, Map.of("type", STRING, DESCRIPTION, "Minimum log level (INFO, DEBUG, ERROR, etc.)")
                                )
                        )
                ),
                Map.of(
                        "name", "stop_logs",
                        DESCRIPTION, "Stop live log streaming",
                        INPUT_SCHEMA, Map.of("type", OBJECT, PROPERTIES, Map.of())
                ),
                Map.of(
                        "name", "get_modules",
                        DESCRIPTION, "Retrieve registered modules and plugins",
                        INPUT_SCHEMA, Map.of("type", OBJECT, PROPERTIES, Map.of())
                ),
                Map.of(
                        "name", "reload_modules",
                        DESCRIPTION, "Reload selected modules/plugins or all when omitted",
                        INPUT_SCHEMA, Map.of(
                                "type", OBJECT,
                                PROPERTIES, Map.of(
                                        MODULES, Map.of("type", "array", "items", Map.of("type", STRING))
                                )
                        )
                ),
                Map.of(
                        "name", "list_caches",
                        DESCRIPTION, "List available local cache names",
                        INPUT_SCHEMA, Map.of("type", OBJECT, PROPERTIES, Map.of())
                ),
                Map.of(
                        "name", "get_cache_entries",
                        DESCRIPTION, "Retrieve entries for a cache by name",
                        INPUT_SCHEMA, Map.of(
                                "type", OBJECT,
                                PROPERTIES, Map.of(NAME, Map.of("type", STRING)),
                                REQUIRED, List.of(NAME)
                        )
                ),
                Map.of(
                        "name", "get_chaos_monkey_config",
                        DESCRIPTION, "Retrieve current chaos monkey configuration",
                        INPUT_SCHEMA, Map.of("type", OBJECT, PROPERTIES, Map.of())
                ),
                Map.of(
                        "name", "configure_chaos_monkey",
                        DESCRIPTION, "Update one chaos monkey assault configuration",
                        INPUT_SCHEMA, Map.of(
                                "type", OBJECT,
                                PROPERTIES, Map.of(
                                        ASSAULT_TYPE, Map.of("type", STRING),
                                        CONFIG_ARG, Map.of("type", OBJECT)
                                ),
                                REQUIRED, List.of(ASSAULT_TYPE, CONFIG_ARG)
                        )
                )
        );
        client.sendResult(id, Map.of("tools", tools));
    }

    private static void handleToolsCall(PortalRegistryWebSocketClient client, Object id, Map<String, Object> params) throws IOException, ParseException {
        ValidationResult validation = validateToolCall(params);
        if (!validation.valid()) {
            client.sendError(id, -32602, validation.errorMessage());
            return;
        }
        String name = validation.toolName();
        Map<String, Object> args = validation.arguments();

        switch (name) {
            case "get_service_info":
                client.sendResult(id, ServerInfoUtil.getServerInfo(ServerInfoConfig.load()));
                break;
            case "check":
                client.sendResult(id, Map.of("result", "OK"));
                break;
            case "get_loggers":
                client.sendResult(id, getLoggers());
                break;
            case "set_loggers": {
                LoggerValidationResult loggers = validateLoggerEntries(args.get(LOGGERS));
                if (!loggers.valid()) {
                    client.sendError(id, -32602, loggers.errorMessage());
                    return;
                }
                setLoggers(loggers.loggers());
                client.sendResult(id, Map.of(STATUS, SUCCESS));
                break;
            }
            case "get_log_content":
                client.sendResult(id, getLogContent(args));
                break;
            case "start_logs":
                startLogs(client, args);
                client.sendResult(id, Map.of(STATUS, SUCCESS, MESSAGE, "Live logs started"));
                break;
            case "stop_logs":
                stopLogs(client);
                client.sendResult(id, Map.of(STATUS, SUCCESS, MESSAGE, "Live logs stopped"));
                break;
            case "get_modules":
                client.sendResult(id, getModules());
                break;
            case "reload_modules":
                client.sendResult(id, reloadModules(args));
                break;
            case "list_caches":
                client.sendResult(id, listCaches());
                break;
            case "get_cache_entries":
                client.sendResult(id, getCacheEntries(args));
                break;
            case "get_chaos_monkey_config":
                client.sendResult(id, getChaosMonkeyConfig());
                break;
            case "configure_chaos_monkey":
                if (!isChaosMonkeyAvailable()) {
                    client.sendError(id, -32602, CHAOS_MONKEY_UNAVAILABLE_MESSAGE);
                    return;
                }
                client.sendResult(id, configureChaosMonkey(args));
                break;
            default:
                client.sendError(id, -32602, "Tool not found: " + name);
        }
    }

    private static ValidationResult validateToolCall(Map<String, Object> params) {
        if (params == null) {
            return ValidationResult.invalid("Missing params for tools/call");
        }
        Object nameObject = params.get(NAME);
        if (!(nameObject instanceof String name) || name.isBlank()) {
            return ValidationResult.invalid("Missing or invalid tool name");
        }
        Object argumentsObject = params.get("arguments");
        if (argumentsObject != null && !(argumentsObject instanceof Map<?, ?>)) {
            return ValidationResult.invalid("Invalid tool arguments");
        }
        return ValidationResult.valid(name, castMapOrEmpty(argumentsObject));
    }

    private static LoggerValidationResult validateLoggerEntries(Object loggersObject) {
        if (loggersObject == null) {
            return LoggerValidationResult.invalid("Missing 'loggers' parameter");
        }
        if (!(loggersObject instanceof List<?> loggersList)) {
            return LoggerValidationResult.invalid("'loggers' must be a list");
        }

        List<Map<String, String>> validatedLoggers = new ArrayList<>();
        int index = 0;
        for (Object entry : loggersList) {
            if (!(entry instanceof Map<?, ?> entryMap)) {
                return LoggerValidationResult.invalid("Each logger entry must be an object with 'name' and 'level' fields (invalid entry at index " + index + ")");
            }
            String loggerName = readRequiredString(entryMap.get(NAME));
            if (loggerName == null) {
                return LoggerValidationResult.invalid("Each logger entry must contain a non-blank 'name' (invalid entry at index " + index + ")");
            }
            String levelString = readRequiredString(entryMap.get(LEVEL));
            if (levelString == null) {
                return LoggerValidationResult.invalid("Each logger entry must contain a non-blank 'level' (invalid entry at index " + index + ")");
            }
            try {
                Level.valueOf(levelString.toUpperCase());
            } catch (IllegalArgumentException e) {
                return LoggerValidationResult.invalid("Invalid logger level '" + levelString + "' for logger '" + loggerName + "' at index " + index);
            }
            Map<String, String> validatedEntry = new HashMap<>();
            validatedEntry.put(NAME, loggerName);
            validatedEntry.put(LEVEL, levelString);
            validatedLoggers.add(validatedEntry);
            index++;
        }
        return LoggerValidationResult.valid(validatedLoggers);
    }

    private static String readRequiredString(Object value) {
        if (!(value instanceof String string) || string.isBlank()) {
            return null;
        }
        return string.trim();
    }

    private static Map<String, Object> castMapOrEmpty(Object value) {
        if (!(value instanceof Map<?, ?> rawMap)) {
            return Collections.emptyMap();
        }
        Map<String, Object> converted = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            converted.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return converted;
    }

    private static List<LoggerInfo> getLoggers() {
        List<LoggerInfo> loggersList = new ArrayList<>();
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (ch.qos.logback.classic.Logger log : lc.getLoggerList()) {
            if (log.getLevel() != null) {
                LoggerInfo loggerInfo = new LoggerInfo();
                loggerInfo.setName(log.getName());
                loggerInfo.setLevel(log.getLevel().toString());
                loggersList.add(loggerInfo);
            }
        }
        return loggersList;
    }

    private static void setLoggers(List<Map<String, String>> loggers) {
        if (loggers == null) return;
        for (Map<String, String> map : loggers) {
            String name = map.get(NAME);
            String levelStr = map.get(LEVEL);
            if (levelStr == null) continue;
            Level level = Level.valueOf(levelStr.toUpperCase());
            ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(name);
            if (level != logger.getLevel()) logger.setLevel(level);
        }
    }

    private static long parseRequiredLongArg(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException e) {
                // fall through to throw below
            }
        }
        throw new IllegalArgumentException("Invalid long parameter: " + key);
    }

    private static long parseOptionalLongArg(Map<String, Object> args, String key, long defaultValue) {
        if (!args.containsKey(key)) {
            return defaultValue;
        }
        Object value = args.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Invalid long parameter: " + key);
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid long parameter: " + key);
            }
        }
        throw new IllegalArgumentException("Invalid long parameter: " + key);
    }

    private static Map<String, Object> getLogContent(Map<String, Object> args) throws IOException, ParseException {
        long startTime = parseRequiredLongArg(args, START_TIME);
        long endTime = parseOptionalLongArg(args, END_TIME, System.currentTimeMillis());
        Level loggerLevel = parseOptionalLevelArg(args, LOGGER_LEVEL, Level.ERROR);

        Map<String, Map<String, Object>> logContent = new HashMap<>();
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (ch.qos.logback.classic.Logger log : lc.getLoggerList()) {
            Map<String, Object> logMap = parseLogContents(startTime, endTime, log, loggerLevel);
            if (hasLogs(logMap)) {
                logContent.put(log.getName(), logMap);
            }
        }
        return Map.of("content", logContent);
    }

    private static Level parseOptionalLevelArg(Map<String, Object> args, String key, Level defaultValue) {
        if (!args.containsKey(key)) {
            return defaultValue;
        }
        Object value = args.get(key);
        if (value == null || !(value instanceof String levelString) || levelString.isBlank()) {
            throw new IllegalArgumentException("Invalid logger level parameter: " + key);
        }
        try {
            return Level.valueOf(levelString.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid logger level parameter: " + key);
        }
    }

    private static Map<String, Object> parseLogContents(long startTime, long endTime, ch.qos.logback.classic.Logger log, Level loggerLevel) throws IOException, ParseException {
        List<Map<String, Object>> allLogs = new ArrayList<>();
        int total = 0;
        for (Iterator<Appender<ILoggingEvent>> it = log.iteratorForAppenders(); it.hasNext(); ) {
            Appender<ILoggingEvent> logEvent = it.next();
            Map<String, Object> appenderResult = parseAppenderLogs(logEvent, startTime, endTime, loggerLevel);
            if (hasLogs(appenderResult)) {
                total += ((Number) appenderResult.get(TOTAL)).intValue();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> logs = (List<Map<String, Object>>) appenderResult.get(LOGS);
                allLogs.addAll(logs);
            }
        }
        if (total == 0 && allLogs.isEmpty()) {
            return Collections.emptyMap();
        }
        return Map.of(TOTAL, total, LOGS, allLogs);
    }

    private static Map<String, Object> parseAppenderLogs(Appender<ILoggingEvent> logEvent, long startTime, long endTime, Level loggerLevel) throws IOException, ParseException {
        if (!(logEvent instanceof RollingFileAppender<?> rollingFileAppender)) {
            return Collections.emptyMap();
        }
        String filePath = rollingFileAppender.getFile();
        if (filePath == null || filePath.isBlank()) {
            return Collections.emptyMap();
        }
        Path logFile = Path.of(filePath);
        if (!Files.exists(logFile)) {
            return Collections.emptyMap();
        }
        try (BufferedReader bufferedReader = Files.newBufferedReader(logFile)) {
            return parseAppenderFile(bufferedReader, startTime, endTime, loggerLevel);
        }
    }

    private static boolean hasLogs(Map<String, Object> result) {
        return result != null
                && result.containsKey(TOTAL)
                && result.containsKey(LOGS)
                && result.get(TOTAL) instanceof Number number
                && number.intValue() > 0
                && result.get(LOGS) instanceof List<?>;
    }

    private static Map<String, Object> parseAppenderFile(BufferedReader bufferedReader, long startTime, long endTime, Level loggerLevel) throws IOException, ParseException {
        List<Map<String, Object>> logs = new ArrayList<>();
        int index = 0;
        String currentLine;
        SimpleDateFormat timestampFormat = new SimpleDateFormat(TIMESTAMP_FORMAT);
        while ((currentLine = bufferedReader.readLine()) != null) {
            try {
                Map<String, Object> logLine = Config.getInstance().getMapper().readValue(currentLine, Map.class);
                if (logLine != null && logLine.containsKey("timestamp") && logLine.containsKey(LEVEL)) {
                    long logTime = timestampFormat.parse(logLine.get("timestamp").toString()).toInstant().toEpochMilli();
                    Level logLevel = Level.valueOf(((String) logLine.get(LEVEL)).trim());
                    if (logTime >= startTime && logTime <= endTime && logLevel.isGreaterOrEqual(loggerLevel)) {
                        logs.add(logLine);
                        index++;
                    }
                }
            } catch (ParseException ignored) {
                // Ignore lines with timestamps that cannot be parsed; continue scanning the log file.
            } catch (RuntimeException ignored) {
                // Ignore malformed lines because log files may contain non-JSON output.
            } catch (IOException ignored) {
                // Ignore lines that cannot be parsed as JSON; continue scanning the log file.
            }
        }
        return Map.of(TOTAL, index, LOGS, logs);
    }

    private static synchronized void startLogs(PortalRegistryWebSocketClient client, Map<String, Object> args) {
        stopLogs(client);
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        Level threshold = parseOptionalLevelArg(args, LEVEL, null);
        McpLogAppender activeLogAppender = new McpLogAppender(client, threshold);
        activeLogAppender.setContext(lc);
        activeLogAppender.start();
        attachLiveAppender(lc, activeLogAppender);
        activeLogAppenders.put(client, activeLogAppender);
        logger.info("Started MCP live log streaming");
    }

    private static synchronized void stopLogs(PortalRegistryWebSocketClient client) {
        McpLogAppender activeLogAppender = activeLogAppenders.remove(client);
        if (activeLogAppender != null) {
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            detachLiveAppender(lc, activeLogAppender);
            activeLogAppender.stop();
            logger.info("Stopped MCP live log streaming");
        }
    }

    private static void attachLiveAppender(LoggerContext loggerContext, McpLogAppender appender) {
        ch.qos.logback.classic.Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(appender);
        for (ch.qos.logback.classic.Logger logger : loggerContext.getLoggerList()) {
            if (logger == root) {
                continue;
            }
            if (!logger.isAdditive()) {
                logger.addAppender(appender);
            }
        }
    }

    private static void detachLiveAppender(LoggerContext loggerContext, McpLogAppender appender) {
        for (ch.qos.logback.classic.Logger logger : loggerContext.getLoggerList()) {
            logger.detachAppender(appender);
        }
    }

    /**
     * Stops and detaches the active log appender if it belongs to the given client.
     * Called when a websocket client disconnects to avoid leaking appenders across reconnects.
     *
     * @param client the websocket client that disconnected
     */
    public static synchronized void stopLogsForClient(PortalRegistryWebSocketClient client) {
        stopLogs(client);
    }

    private static Map<String, Object> getModules() {
        List<String> modules = new ArrayList<>();
        modules.addAll(ModuleRegistry.getModuleClasses());
        modules.addAll(ModuleRegistry.getPluginClasses());
        return Map.of(MODULES, modules);
    }

    private static Map<String, Object> reloadModules(Map<String, Object> args) {
        List<String> requestedModules = readOptionalStringList(args.get(MODULES), MODULES);
        List<String> modulesToReload = new ArrayList<>();
        if (requestedModules == null || requestedModules.isEmpty() || isReloadAll(requestedModules)) {
            modulesToReload.addAll(ModuleRegistry.getModuleClasses());
            modulesToReload.addAll(ModuleRegistry.getPluginClasses());
        } else {
            modulesToReload.addAll(requestedModules);
        }

        List<String> reloaded = new ArrayList<>();
        if (!modulesToReload.isEmpty()) {
            Config.getInstance().clearConfigCache("values");
            logger.info("Centralized config values.yml is reloaded.");
        }

        for (String moduleClass : modulesToReload) {
            String configName = findConfigName(moduleClass);
            if (configName == null) {
                logger.warn("Module or plugin {} is not found in the registry", moduleClass);
                continue;
            }
            Config.getInstance().clearConfigCache(configName);
            reloaded.add(moduleClass);
        }
        return Map.of(MODULES, reloaded);
    }

    private static Map<String, Object> listCaches() {
        if (!isCacheAvailable()) {
            return Map.of(
                    SUPPORTED, false,
                    MESSAGE, CACHE_UNAVAILABLE_MESSAGE,
                    CACHES, Collections.emptyList()
            );
        }
        try {
            Class<?> cacheConfigClass = resolveClass("com.networknt.cache.CacheConfig");
            Method loadMethod = cacheConfigClass.getMethod("load");
            Object cacheConfig = loadMethod.invoke(null);
            Method getCachesMethod = cacheConfigClass.getMethod("getCaches");
            @SuppressWarnings("unchecked")
            List<Object> cacheItems = (List<Object>) getCachesMethod.invoke(cacheConfig);
            List<String> cacheNames = new ArrayList<>();
            if (cacheItems != null) {
                for (Object cacheItem : cacheItems) {
                    Method getCacheNameMethod = cacheItem.getClass().getMethod("getCacheName");
                    cacheNames.add(String.valueOf(getCacheNameMethod.invoke(cacheItem)));
                }
            }
            return Map.of(
                    SUPPORTED, true,
                    CACHES, cacheNames
            );
        } catch (IllegalStateException e) {
            logger.info("Cache support is not available", e);
            return Map.of(
                    SUPPORTED, false,
                    MESSAGE, CACHE_UNAVAILABLE_MESSAGE,
                    CACHES, Collections.emptyList()
            );
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to list caches", e);
        }
    }

    private static Map<String, Object> getCacheEntries(Map<String, Object> args) {
        String cacheName = readRequiredString(args.get(NAME));
        if (cacheName == null) {
            throw new IllegalArgumentException("Missing required parameter: name");
        }
        if (!isCacheAvailable()) {
            return Map.of(
                    SUPPORTED, false,
                    MESSAGE, CACHE_UNAVAILABLE_MESSAGE,
                    NAME, cacheName,
                    ENTRIES, Collections.emptyMap()
            );
        }
        try {
            Class<?> cacheManagerClass = resolveClass("com.networknt.cache.CacheManager");
            Method getInstanceMethod = cacheManagerClass.getMethod("getInstance");
            Object cacheManager = getInstanceMethod.invoke(null);
            if (cacheManager == null) {
                return Map.of(
                        SUPPORTED, false,
                        MESSAGE, CACHE_UNAVAILABLE_MESSAGE,
                        NAME, cacheName,
                        ENTRIES, Collections.emptyMap()
                );
            }
            Method method = cacheManagerClass.getMethod("getCache", String.class);
            @SuppressWarnings("unchecked")
            Map<Object, Object> cacheMap = (Map<Object, Object>) method.invoke(cacheManager, cacheName);
            return Map.of(
                    SUPPORTED, true,
                    NAME, cacheName,
                    ENTRIES, cacheMap == null ? Collections.emptyMap() : summarizeCacheEntries(cacheMap)
            );
        } catch (IllegalStateException e) {
            logger.info("Cache support is not available", e);
            return Map.of(
                    SUPPORTED, false,
                    MESSAGE, CACHE_UNAVAILABLE_MESSAGE,
                    NAME, cacheName,
                    ENTRIES, Collections.emptyMap()
            );
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to retrieve cache entries", e);
        }
    }

    private static Map<String, Object> getChaosMonkeyConfig() {
        if (!isChaosMonkeyAvailable()) {
            return Map.of(
                    SUPPORTED, false,
                    MESSAGE, CHAOS_MONKEY_UNAVAILABLE_MESSAGE
            );
        }
        Map<String, Object> configMap = new HashMap<>();
        configMap.put(SUPPORTED, true);
        configMap.put(EXCEPTION, getChaosConfig(EXCEPTION_ASSAULT_HANDLER, CONFIG_ARG));
        configMap.put(KILLAPP, getChaosConfig(KILLAPP_ASSAULT_HANDLER, CONFIG_ARG));
        configMap.put(LATENCY, getChaosConfig(LATENCY_ASSAULT_HANDLER, CONFIG_ARG));
        configMap.put(MEMORY, getChaosConfig(MEMORY_ASSAULT_HANDLER, CONFIG_ARG));
        return configMap;
    }

    private static Map<String, Object> configureChaosMonkey(Map<String, Object> args) {
        String assaultType = readRequiredString(args.get(ASSAULT_TYPE));
        if (assaultType == null) {
            throw new IllegalArgumentException("Missing required parameter: assaultType");
        }
        Object configObject = args.get(CONFIG_ARG);
        if (!(configObject instanceof Map<?, ?> rawConfig)) {
            throw new IllegalArgumentException("Missing or invalid parameter: config");
        }
        Map<String, Object> config = castMapOrEmpty(rawConfig);
        String handlerClassName = resolveAssaultHandlerClass(assaultType);
        try {
            Class<?> handlerClass = resolveClass(handlerClassName);
            String configClassName = handlerClassName.replace("Handler", capitalize(CONFIG_ARG));
            Class<?> configClass = resolveClass(configClassName);
            Object convertedConfig = Config.getInstance().getMapper().convertValue(config, configClass);
            Field configField = handlerClass.getField("config");
            configField.set(null, convertedConfig);
            return Map.of(ASSAULT_TYPE, normalizeAssaultId(assaultType), CONFIG_ARG, config);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to configure chaos monkey assault " + assaultType, e);
        }
    }

    private static Object getChaosConfig(String handlerClassName, String fieldName) {
        try {
            Class<?> handlerClass = resolveClass(handlerClassName);
            Field configField = handlerClass.getField(fieldName);
            Object currentConfig = configField.get(null);
            if (currentConfig != null) {
                return currentConfig;
            }
            String configClassName = handlerClassName.replace("Handler", capitalize(fieldName));
            Class<?> configClass = resolveClass(configClassName);
            Method loadMethod = configClass.getMethod("load");
            return loadMethod.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to load chaos monkey config for " + handlerClassName, e);
        }
    }

    private static String resolveAssaultHandlerClass(String assaultType) {
        String normalized = normalizeAssaultId(assaultType);
        return switch (normalized) {
            case EXCEPTION -> EXCEPTION_ASSAULT_HANDLER;
            case KILLAPP -> KILLAPP_ASSAULT_HANDLER;
            case LATENCY -> LATENCY_ASSAULT_HANDLER;
            case MEMORY -> MEMORY_ASSAULT_HANDLER;
            default -> throw new IllegalArgumentException("Invalid assaultType: " + assaultType);
        };
    }

    private static String normalizeAssaultId(String assaultType) {
        String normalized = assaultType == null ? "" : assaultType.trim().toLowerCase();
        if ("killappassaulthandler".equals(normalized) || KILLAPP_ASSAULT_HANDLER.toLowerCase().equals(normalized) || KILLAPP.equals(normalized) || "kill_app".equals(normalized)) {
            return KILLAPP;
        }
        if ("exceptionassaulthandler".equals(normalized) || EXCEPTION_ASSAULT_HANDLER.toLowerCase().equals(normalized) || EXCEPTION.equals(normalized)) {
            return EXCEPTION;
        }
        if ("latencyassaulthandler".equals(normalized) || LATENCY_ASSAULT_HANDLER.toLowerCase().equals(normalized) || LATENCY.equals(normalized)) {
            return LATENCY;
        }
        if ("memoryassaulthandler".equals(normalized) || MEMORY_ASSAULT_HANDLER.toLowerCase().equals(normalized) || MEMORY.equals(normalized)) {
            return MEMORY;
        }
        return normalized;
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static boolean isReloadAll(List<String> modules) {
        return modules.size() == 1 && "all".equalsIgnoreCase(modules.getFirst());
    }

    private static boolean isCacheAvailable() {
        try {
            resolveClass("com.networknt.cache.CacheConfig");
            resolveClass("com.networknt.cache.CacheManager");
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private static Map<String, Object> summarizeCacheEntries(Map<?, ?> rawMap) {
        Map<String, Object> summaryMap = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            summaryMap.put(String.valueOf(entry.getKey()), summarizeCacheValue(entry.getValue()));
        }
        return summaryMap;
    }

    private static Map<String, Object> summarizeCacheValue(Object value) {
        if (value == null) {
            return Map.of(TYPE, "null");
        }
        if (value instanceof Number || value instanceof Boolean) {
            return Map.of(TYPE, value.getClass().getSimpleName().toLowerCase());
        }
        if (value instanceof String string) {
            return Map.of(TYPE, STRING, SIZE, string.length());
        }
        if (value instanceof Map<?, ?> map) {
            List<String> keys = new ArrayList<>();
            int count = 0;
            for (Object key : map.keySet()) {
                if (count++ >= 10) {
                    break;
                }
                keys.add(String.valueOf(key));
            }
            return Map.of(
                    TYPE, OBJECT,
                    SIZE, map.size(),
                    KEYS, keys
            );
        }
        if (value instanceof Iterable<?> iterable) {
            int count = 0;
            for (Object ignored : iterable) {
                count++;
            }
            return Map.of(TYPE, "array", SIZE, count);
        }
        if (value.getClass().isArray()) {
            return Map.of(TYPE, "array", SIZE, Array.getLength(value));
        }
        return Map.of(TYPE, value.getClass().getName());
    }

    private static List<String> readOptionalStringList(Object value, String key) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (!(value instanceof List<?> values)) {
            throw new IllegalArgumentException("'" + key + "' must be a list");
        }
        List<String> result = new ArrayList<>();
        for (Object entry : values) {
            if (!(entry instanceof String string) || string.isBlank()) {
                throw new IllegalArgumentException("Each entry in '" + key + "' must be a non-blank string");
            }
            result.add(string.trim());
        }
        return result;
    }

    private static String findConfigName(String moduleClass) {
        for (String key : ModuleRegistry.getModuleRegistry().keySet()) {
            if (key.endsWith(":" + moduleClass)) {
                return key.substring(0, key.lastIndexOf(':'));
            }
        }
        for (String key : ModuleRegistry.getPluginRegistry().keySet()) {
            if (key.endsWith(":" + moduleClass)) {
                return key.substring(0, key.lastIndexOf(':'));
            }
        }
        return null;
    }

    private static Class<?> resolveClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Required class is not available: " + className, e);
        }
    }

    private static boolean isChaosMonkeyAvailable() {
        try {
            resolveClass(EXCEPTION_ASSAULT_HANDLER);
            resolveClass(KILLAPP_ASSAULT_HANDLER);
            resolveClass(LATENCY_ASSAULT_HANDLER);
            resolveClass(MEMORY_ASSAULT_HANDLER);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private record ValidationResult(boolean valid, String toolName, Map<String, Object> arguments, String errorMessage) {
        private static ValidationResult valid(String toolName, Map<String, Object> arguments) {
            return new ValidationResult(true, toolName, arguments, null);
        }

        private static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, null, Collections.emptyMap(), errorMessage);
        }
    }

    private record LoggerValidationResult(boolean valid, List<Map<String, String>> loggers, String errorMessage) {
        private static LoggerValidationResult valid(List<Map<String, String>> loggers) {
            return new LoggerValidationResult(true, loggers, null);
        }

        private static LoggerValidationResult invalid(String errorMessage) {
            return new LoggerValidationResult(false, Collections.emptyList(), errorMessage);
        }
    }
}
