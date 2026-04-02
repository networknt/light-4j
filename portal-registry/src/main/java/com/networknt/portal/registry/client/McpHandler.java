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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class McpHandler {
    private static final Logger logger = LoggerFactory.getLogger(McpHandler.class);
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ";
    private static final String OBJECT = "object";
    private static final String PROPERTIES = "properties";
    private static final String INPUT_SCHEMA = "inputSchema";
    private static final String DESCRIPTION = "description";
    private static final String LOGGERS = "loggers";
    private static final String STRING = "string";
    private static final String START_TIME = "startTime";
    private static final String END_TIME = "endTime";
    private static final String LOGGER_LEVEL = "loggerLevel";
    private static final String LEVEL = "level";
    private static final String NAME = "name";
    private static final String STATUS = "status";
    private static final String SUCCESS = "success";
    private static final String TOTAL = "total";
    private static final String LOGS = "logs";
    private static McpLogAppender activeLogAppender = null;

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
        } catch (Exception e) {
            logger.error("Error handling MCP request", e);
            client.sendError(id, -32603, e.getMessage());
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
                                "required", List.of(LOGGERS)
                        )
                ),
                Map.of(
                        "name", "get_log_content",
                        DESCRIPTION, "Retrieve historical log content",
                        INPUT_SCHEMA, Map.of(
                                "type", OBJECT,
                                PROPERTIES, Map.of(
                                        START_TIME, Map.of("type", STRING),
                                        LOGGER_LEVEL, Map.of("type", STRING)
                                ),
                                "required", List.of(START_TIME)
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
                client.sendResult(id, Map.of(STATUS, SUCCESS, "message", "Live logs started"));
                break;
            case "stop_logs":
                stopLogs();
                client.sendResult(id, Map.of(STATUS, SUCCESS, "message", "Live logs stopped"));
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
            Level level = Level.valueOf(map.get(LEVEL));
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
        Level loggerLevel = args.containsKey(LOGGER_LEVEL) ? Level.toLevel((String) args.get(LOGGER_LEVEL)) : Level.ERROR;

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
        Path logFile = Path.of(rollingFileAppender.getFile());
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
            } catch (ParseException e) {
                throw e;
            } catch (RuntimeException ignored) {
                // Ignore malformed lines because log files may contain non-JSON output.
            } catch (IOException ignored) {
                // Ignore lines that cannot be parsed as JSON; continue scanning the log file.
            }
        }
        return Map.of(TOTAL, index, LOGS, logs);
    }

    private static synchronized void startLogs(PortalRegistryWebSocketClient client, Map<String, Object> args) {
        if (activeLogAppender != null) {
            stopLogs();
        }
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        activeLogAppender = new McpLogAppender(client);
        activeLogAppender.setContext(lc);
        String levelStr = (String) args.get(LEVEL);
        if (levelStr != null) {
            // The stream currently attaches at the root logger and does not filter by threshold yet.
        }
        activeLogAppender.start();
        ch.qos.logback.classic.Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(activeLogAppender);
        logger.info("Started MCP live log streaming");
    }

    private static synchronized void stopLogs() {
        if (activeLogAppender != null) {
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
            root.detachAppender(activeLogAppender);
            activeLogAppender.stop();
            activeLogAppender = null;
            logger.info("Stopped MCP live log streaming");
        }
    }

    /**
     * Stops and detaches the active log appender if it belongs to the given client.
     * Called when a websocket client disconnects to avoid leaking appenders across reconnects.
     *
     * @param client the websocket client that disconnected
     */
    public static synchronized void stopLogsForClient(PortalRegistryWebSocketClient client) {
        if (activeLogAppender != null && activeLogAppender.isForClient(client)) {
            stopLogs();
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
