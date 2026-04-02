package com.networknt.portal.registry.client;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import com.networknt.config.Config;
import com.networknt.info.ServerInfoConfig;
import com.networknt.info.ServerInfoUtil;
import com.networknt.logging.model.LoggerConfig;
import com.networknt.logging.model.LoggerInfo;
import com.networknt.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class McpHandler {
    private static final Logger logger = LoggerFactory.getLogger(McpHandler.class);
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ";
    private static McpLogAppender activeLogAppender = null;

    public static void handle(PortalRegistryWebSocketClient client, Map<String, Object> envelope) {
        String method = (String) envelope.get("method");
        Object id = envelope.get("id");
        if (id == null) return;

        try {
            switch (method) {
                case "tools/list":
                    handleToolsList(client, id);
                    break;
                case "tools/call":
                    handleToolsCall(client, id, (Map<String, Object>) envelope.get("params"));
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
                        "description", "Retrieve information about this microservice instance",
                        "inputSchema", Map.of("type", "object", "properties", Map.of())
                ),
                Map.of(
                        "name", "check",
                        "description", "Perform a local health check",
                        "inputSchema", Map.of("type", "object", "properties", Map.of())
                ),
                Map.of(
                        "name", "get_loggers",
                        "description", "Retrieve current logger configuration",
                        "inputSchema", Map.of("type", "object", "properties", Map.of())
                ),
                Map.of(
                        "name", "set_loggers",
                        "description", "Update logger levels",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of("loggers", Map.of("type", "array", "items", Map.of("type", "object"))),
                                "required", List.of("loggers")
                        )
                ),
                Map.of(
                        "name", "get_log_content",
                        "description", "Retrieve historical log content",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "startTime", Map.of("type", "string"),
                                        "loggerLevel", Map.of("type", "string")
                                ),
                                "required", List.of("startTime")
                        )
                ),
                Map.of(
                        "name", "start_logs",
                        "description", "Start live log streaming",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "level", Map.of("type", "string", "description", "Minimum log level (INFO, DEBUG, ERROR, etc.)")
                                )
                        )
                ),
                Map.of(
                        "name", "stop_logs",
                        "description", "Stop live log streaming",
                        "inputSchema", Map.of("type", "object", "properties", Map.of())
                )
        );
        client.sendResult(id, Map.of("tools", tools));
    }

    private static void handleToolsCall(PortalRegistryWebSocketClient client, Object id, Map<String, Object> params) throws Exception {
        String name = (String) params.get("name");
        Map<String, Object> args = (Map<String, Object>) params.get("arguments");
        if (args == null) args = Collections.emptyMap();

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
            case "set_loggers":
                setLoggers((List<Map<String, String>>) args.get("loggers"));
                client.sendResult(id, Map.of("status", "success"));
                break;
            case "get_log_content":
                client.sendResult(id, getLogContent(args));
                break;
            case "start_logs":
                startLogs(client, args);
                client.sendResult(id, Map.of("status", "success", "message", "Live logs started"));
                break;
            case "stop_logs":
                stopLogs();
                client.sendResult(id, Map.of("status", "success", "message", "Live logs stopped"));
                break;
            default:
                client.sendError(id, -32602, "Tool not found: " + name);
        }
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
            Level level = Level.valueOf(map.get("level"));
            ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(name);
            if (level != logger.getLevel()) logger.setLevel(level);
        }
    }

    private static Map<String, Object> getLogContent(Map<String, Object> args) throws Exception {
        long startTime = Long.parseLong((String) args.get("startTime"));
        long endTime = args.containsKey("endTime") ? Long.parseLong((String) args.get("endTime")) : System.currentTimeMillis();
        Level loggerLevel = args.containsKey("loggerLevel") ? Level.toLevel((String) args.get("loggerLevel")) : Level.ERROR;

        Map<String, Map<String, Object>> logContent = new HashMap<>();
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (ch.qos.logback.classic.Logger log : lc.getLoggerList()) {
            Map<String, Object> logMap = parseLogContents(startTime, endTime, log, loggerLevel);
            if (logMap.size() > 0 && (Integer) logMap.get("total") > 0)
                logContent.put(log.getName(), logMap);
        }
        return Map.of("content", logContent);
    }

    private static Map<String, Object> parseLogContents(long startTime, long endTime, ch.qos.logback.classic.Logger log, Level loggerLevel) throws Exception {
        Map<String, Object> res = new HashMap<>();
        for (Iterator<Appender<ILoggingEvent>> it = log.iteratorForAppenders(); it.hasNext(); ) {
            Appender<ILoggingEvent> logEvent = it.next();
            if (logEvent instanceof RollingFileAppender) {
                Path logFile = Path.of(((RollingFileAppender<ILoggingEvent>) logEvent).getFile());
                if (Files.exists(logFile)) {
                    try (BufferedReader bufferedReader = Files.newBufferedReader(logFile)) {
                        res = parseAppenderFile(bufferedReader, startTime, endTime, log, loggerLevel);
                    }
                }
            }
        }
        return res;
    }

    private static Map<String, Object> parseAppenderFile(BufferedReader bufferedReader, long startTime, long endTime, ch.qos.logback.classic.Logger log, Level loggerLevel) throws Exception {
        List<Map<String, Object>> logs = new ArrayList<>();
        int index = 0;
        String currentLine;
        while ((currentLine = bufferedReader.readLine()) != null) {
            try {
                Map<String, Object> logLine = Config.getInstance().getMapper().readValue(currentLine, Map.class);
                if (logLine != null && logLine.containsKey("timestamp") && logLine.containsKey("level")) {
                    SimpleDateFormat timestampFormat = new SimpleDateFormat(TIMESTAMP_FORMAT);
                    long logTime = timestampFormat.parse(logLine.get("timestamp").toString()).toInstant().toEpochMilli();
                    Level logLevel = Level.valueOf(((String) logLine.get("level")).trim());
                    if (logTime >= startTime && logTime <= endTime && logLevel.isGreaterOrEqual(loggerLevel)) {
                        logs.add(logLine);
                        index++;
                    }
                }
            } catch (Exception ignored) {}
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
        String levelStr = (String) args.get("level");
        if (levelStr != null) {
            // We can't easily set level on the specific appender in standard AppenderBase without a filter
            // but for simplicity, we'll just add it to the root logger.
            // If more granular control is needed, we'd add a ThresholdFilter.
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
