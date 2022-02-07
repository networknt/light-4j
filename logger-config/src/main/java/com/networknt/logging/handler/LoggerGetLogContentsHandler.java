package com.networknt.logging.handler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.Config;
import com.networknt.handler.LightHttpHandler;
import com.networknt.httpstring.ContentType;
import com.networknt.logging.model.LoggerConfig;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Get logs from the log files based on the input parameters.
 *
 */
public class LoggerGetLogContentsHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(LoggerGetLogContentsHandler.class);
    public static final String CONFIG_NAME = "logging";
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ";
    static final String STATUS_LOGGER_INFO_DISABLED = "ERR12108";
    static final String STATUS_LOGGER_FILE_INVALID = "ERR12110";
    static final String TIMESTAMP_LOG_KEY = "timestamp";
    static final String LEVEL_LOG_KEY = "level";
    static final String LOGGER_LOG_KEY = "logger";
    static final String ROOT_LOGGER_NAME = "ROOT";
    public static final int DEFAULT_LIMIT = 100;
    public static final int DEFAULT_OFFSET = 0;

    private static final ObjectMapper mapper = Config.getInstance().getMapper();

    @Override
    public void handleRequest(HttpServerExchange exchange) throws IOException, ParseException {
        LoggerConfig config = (LoggerConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, LoggerConfig.class);
        long requestTimeRangeStart = System.currentTimeMillis()- config.getLogStart();
        long requestTimeRangeEnd = System.currentTimeMillis();
        int limit = DEFAULT_LIMIT;
        int offset = DEFAULT_OFFSET;

        Map<String, Deque<String>> parameters = exchange.getQueryParameters();
        String loggerName = parameters.containsKey("loggerName")? parameters.get("loggerName").getFirst() : null;
        Level loggerLevel = parameters.containsKey("loggerLevel")? Level.toLevel(parameters.get("loggerLevel").getFirst(), Level.ERROR): Level.ERROR;

        if (config.isEnabled()) {
            if(parameters.containsKey("limit"))
                limit = Integer.parseInt(parameters.get("limit").getFirst());
            if(parameters.containsKey("offset"))
                offset = Integer.parseInt(parameters.get("offset").getFirst());
            if(parameters.containsKey("startTime"))
                requestTimeRangeStart = Long.parseLong(parameters.get("startTime").getFirst());
            if(parameters.containsKey("endTime"))
                requestTimeRangeEnd = Long.parseLong(parameters.get("endTime").getFirst());
            if(logger.isDebugEnabled()) logger.debug("startTime = " + requestTimeRangeStart + " endTime = " + requestTimeRangeEnd + " loggerName = " + loggerName + " loggerLevel = " + loggerLevel + " offset = " + offset + " limit = " + limit);
            this.getLogEntries(requestTimeRangeStart, requestTimeRangeEnd, exchange, loggerName, loggerLevel, offset, limit);
        } else {
            logger.error("Logging is disabled in logging.yml");
            setExchangeStatus(exchange, STATUS_LOGGER_INFO_DISABLED);
        }
    }



    /**
     * Look for File Appender log contexts from all logger instances found.
     *
     * @param startTime - the request start time range when grabbing log entries
     * @param endTime - the request end time range when grabbing log entries
     * @param exchange - HttpServer exchange
     */
    private void getLogEntries(long startTime, long endTime, HttpServerExchange exchange, String loggerName, Level loggerLevel, int offset, int limit) throws IOException, ParseException {
        // the return is a map of list with key is the loggerName. There might be multiple of loggers in the logback.xml file.
        Map<String, Map<String, Object>> logContent = new HashMap<>();

        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (ch.qos.logback.classic.Logger log : lc.getLoggerList()) {
            /* only parse the context if the log is valid */
            if (loggerName == null || log.getName().equalsIgnoreCase(loggerName)) {
                Map<String, Object> logMap = this.parseLogContents(startTime, endTime, log, loggerLevel, offset, limit);
                if(logMap.size() > 0 && (Integer)logMap.get("total") > 0)
                    logContent.put(log.getName(), logMap);
            }
        }
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, ContentType.APPLICATION_JSON.value());
        exchange.getResponseSender().send(mapper.writeValueAsString(logContent));
    }

    /**
     *
     * @param startTime - the request start time range when grabbing log entries
     * @param endTime - the request end time range when grabbing log entries
     * @param log - the log context
     * @return - returns the string response for log entry request.
     */
    private Map<String, Object> parseLogContents(long startTime, long endTime, ch.qos.logback.classic.Logger log, Level loggerLevel, int offset, int limit) throws IOException, ParseException {
        Map<String, Object> res = new HashMap<>();
        for(Iterator<Appender<ILoggingEvent>> it = log.iteratorForAppenders(); it.hasNext();) {
            Appender<ILoggingEvent> logEvent = it.next();
            if(logEvent.getClass().equals(RollingFileAppender.class)) {
                FileReader reader = new FileReader(((RollingFileAppender<ILoggingEvent>) logEvent).getFile());
                BufferedReader bufferedReader = new BufferedReader(reader);
                res = this.parseAppenderFile(bufferedReader, startTime, endTime, log, loggerLevel, offset, limit);
            }
        }
        return res;
    }

    /**
     * Goes line-by-line in our log file and looks for entries that are within the bounds of our given time.
     *
     * @param bufferedReader - buffered reader containing our current log file
     * @param startTime - the request start time range when grabbing log entries
     * @param endTime - the request end time range when grabbing log entries
     * @return - returns the string response for this log file
     * @throws ParseException - exception when parsing the file
     * @throws IOException - exception when trying to load the file
     */
    private Map<String, Object> parseAppenderFile(BufferedReader bufferedReader, long startTime, long endTime, ch.qos.logback.classic.Logger log, Level loggerLevel, int offset, int limit) throws ParseException, IOException {
        Map<String, Object> logMap = new HashMap<>();
        List<Map<String, Object>> logs = new ArrayList<>();
        int index = 0;
        String currentLine;
        try {
            while ((currentLine = bufferedReader.readLine()) != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> logLine = mapper.readValue(currentLine, Map.class);
                /* Grab the log entry as a map, and check to see if the timestamp falls within range of startTime and endTime */
                if (logLine != null && logLine.containsKey(TIMESTAMP_LOG_KEY) && logLine.containsKey(LEVEL_LOG_KEY) && logLine.containsKey(LOGGER_LOG_KEY)) {
                    SimpleDateFormat timestampFormat = new SimpleDateFormat(TIMESTAMP_FORMAT);
                    long logTime = timestampFormat.parse(logLine.get(TIMESTAMP_LOG_KEY).toString()).toInstant().toEpochMilli();
                    String levelStr = (String)logLine.get(LEVEL_LOG_KEY);
                    Level logLevel = Level.valueOf(levelStr.trim());
                    String logLogger = (String)logLine.get(LOGGER_LOG_KEY);
                    if (logTime > startTime && logTime < endTime && logLevel.isGreaterOrEqual(loggerLevel) && (log.getName().equals(ROOT_LOGGER_NAME) || logLogger.startsWith(log.getName()))) {
                        if(index >= offset && logs.size() < limit) {
                            logs.add(logLine);
                        }
                        // the total number of entries between the startTime and endTime.
                        index++;
                    }
                }
            }
        } catch (Exception e) {
            // any exception here might be the format is not JSON for the logger. For example Audit logger etc. Ignore it.
        }
        logMap.put("total", index);
        logMap.put("logs", logs);
        return logMap;
    }
}
