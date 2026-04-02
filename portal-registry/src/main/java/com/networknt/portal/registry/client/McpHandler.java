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
    private static final String LOGGER_LEVEL = "loggerLevel";
    private static final String LEVEL = "level";
    private static final String STATUS = "status";
    private static final String SUCCESS = "success";
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
        if (params == null) {
            client.sendError(id, -32602, "Missing params for tools/call");
            return;
        }
        Object nameObject = params.get("name");
        if (!(nameObject instanceof String name) || name.isBlank()) {
            client.sendError(id, -32602, "Missing or invalid tool name");
            return;
        }
        Object argumentsObject = params.get("arguments");
        if (argumentsObject != null && !(argumentsObject instanceof Map<?, ?>)) {
            client.sendError(id, -32602, "Invalid tool arguments");
            return;
        }

        Map<String, Object> args = castMapOrEmpty(argumentsObject);

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
                Object loggersObject = args.get(LOGGERS);
                if (loggersObject == null) {
                    client.sendError(id, -32602, "Missing 'loggers' parameter");
                    return;
                }
                if (!(loggersObject instanceof List<?> loggersList)) {
                    client.sendError(id, -32602, "'loggers' must be a list");
                    return;
                }

                List<Map<String, String>> validatedLoggers = new ArrayList<>();

                int index = 0;
                for (Object entry : loggersList) {
                    if (!(entry instanceof Map<?, ?> entryMap)) {
                        client.sendError(id, -32602, "Each logger entry must be an object with 'name' and 'level' fields (invalid entry at index " + index + ")");
                        return;
                    }
                    Object nameValue = entryMap.get("name");
                    Object levelValue = entryMap.get("level");
                    if (!(nameValue instanceof String) || ((String) nameValue).isBlank()) {
                        client.sendError(id, -32602, "Each logger entry must contain a non-blank 'name' (invalid entry at index " + index + ")");
                        return;
                    }
                    if (!(levelValue instanceof String) || ((String) levelValue).isBlank()) {
                        client.sendError(id, -32602, "Each logger entry must contain a non-blank 'level' (invalid entry at index " + index + ")");
                        return;
                    }
                    String loggerName = ((String) nameValue).trim();
                    String levelString = ((String) levelValue).trim();
                    try {
                        // Validate that the level is a recognized Logback level.
                        Level.valueOf(levelString.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        client.sendError(id, -32602, "Invalid logger level '" + levelString + "' for logger '" + loggerName + "' at index " + index);
                        return;
                    }
                    Map<String, String> validatedEntry = new HashMap<>();
                    validatedEntry.put("name", loggerName);
                    validatedEntry.put("level", levelString);
                    validatedLoggers.add(validatedEntry);
                    index++;
                }

                setLoggers(validatedLoggers);
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
            String name = map.get("name");
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
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
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
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid long parameter: " + key);
            }
        }
        throw new IllegalArgumentException("Invalid long parameter: " + key);
    }

    private static Map<String, Object> getLogContent(Map<String, Object> args) throws IOException, ParseException {
        long startTime = parseRequiredLongArg(args, START_TIME);
        long endTime = parseOptionalLongArg(args, "endTime", System.currentTimeMillis());
        Level loggerLevel = args.containsKey(LOGGER_LEVEL) ? Level.toLevel((String) args.get(LOGGER_LEVEL)) : Level.ERROR;

        Map<String, Map<String, Object>> logContent = new HashMap<>();
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (ch.qos.logback.classic.Logger log : lc.getLoggerList()) {
            Map<String, Object> logMap = parseLogContents(startTime, endTime, log, loggerLevel);
            if (logMap.size() > 0 && (Integer) logMap.get("total") > 0)
                logContent.put(log.getName(), logMap);
        }
        return Map.of("content", logContent);
    }

    private static Map<String, Object> parseLogContents(long startTime, long endTime, ch.qos.logback.classic.Logger log, Level loggerLevel) throws IOException, ParseException {
        List<Map<String, Object>> allLogs = new ArrayList<>();
        int total = 0;
        for (Iterator<Appender<ILoggingEvent>> it = log.iteratorForAppenders(); it.hasNext(); ) {
            Appender<ILoggingEvent> logEvent = it.next();
            if (logEvent instanceof RollingFileAppender) {
                Path logFile = Path.of(((RollingFileAppender<ILoggingEvent>) logEvent).getFile());
                if (Files.exists(logFile)) {
                    try (BufferedReader bufferedReader = Files.newBufferedReader(logFile)) {
                        Map<String, Object> appenderResult = parseAppenderFile(bufferedReader, startTime, endTime, loggerLevel);
                        if (appenderResult != null && appenderResult.containsKey("total") && appenderResult.containsKey("logs")) {
                            Object totalObj = appenderResult.get("total");
                            Object logsObj = appenderResult.get("logs");
                            if (totalObj instanceof Integer && logsObj instanceof List) {
                                total += (Integer) totalObj;
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> logs = (List<Map<String, Object>>) logsObj;
                                allLogs.addAll(logs);
                            }
                        }
                    }
                }
            }
        }
        if (total == 0 && allLogs.isEmpty()) {
            return Collections.emptyMap();
        }
        return Map.of("total", total, "logs", allLogs);
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
        return Map.of("total", index, "logs", logs);
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
}
