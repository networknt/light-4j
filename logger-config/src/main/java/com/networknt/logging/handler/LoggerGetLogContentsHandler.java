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
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class LoggerGetLogContentsHandler implements LightHttpHandler {

    public static final String CONFIG_NAME = "logging";
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ";
    static final String STATUS_LOGGER_INFO_DISABLED = "ERR12108";
    static final String STATUS_LOGGER_FILE_INVALID = "ERR12110";
    static final String TIMESTAMP_LOG_KEY = "timestamp";
    private static final ObjectMapper mapper = Config.getInstance().getMapper();

    @Override
    public void handleRequest(HttpServerExchange exchange) throws IOException, ParseException {
        LoggerConfig config = (LoggerConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, LoggerConfig.class);
        long requestTimeRangeStart = System.currentTimeMillis()- config.getLogStart();
        long requestTimeRangeEnd = System.currentTimeMillis();

        Map<String, Deque<String>> parameters = exchange.getQueryParameters();
        String loggerName = parameters.containsKey("loggerName")? parameters.get("loggerName").getFirst() : null;
        Level loggerLevel = parameters.containsKey("loggerLevel")? Level.toLevel(parameters.get("loggerLevel").getFirst(), Level.ERROR): Level.ERROR;

        if (config.isEnabled()) {

            if(parameters.containsKey("startTime"))
                requestTimeRangeStart = Long.parseLong(parameters.get("startTime").getFirst());
            if(parameters.containsKey("endTime"))
                requestTimeRangeEnd = Long.parseLong(parameters.get("endTime").getFirst());

            this.getLogEntries(requestTimeRangeStart, requestTimeRangeEnd, exchange, loggerName, loggerLevel);
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
    private void getLogEntries(long startTime, long endTime, HttpServerExchange exchange, String loggerName, Level loggerLevel) throws IOException, ParseException {
        List<Object> logContent = new ArrayList<>();

        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (ch.qos.logback.classic.Logger log : lc.getLoggerList()) {

            /* only parse the context if the log is valid */
            if (log.getLevel() != null && log.getLevel().isGreaterOrEqual(loggerLevel) && (loggerName==null || log.getName().equalsIgnoreCase(loggerName))) {
                List<Map<String, Object>> logs = this.parseLogContents(startTime, endTime, log);
                if(!logs.isEmpty())
                    logContent.addAll(logs);
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
    private List<Map<String, Object>> parseLogContents(long startTime, long endTime, ch.qos.logback.classic.Logger log) throws IOException, ParseException {
        List<Map<String, Object>> res = new ArrayList<>();
        for(Iterator<Appender<ILoggingEvent>> it = log.iteratorForAppenders(); it.hasNext();) {
            Appender<ILoggingEvent> logEvent = it.next();
            if(logEvent.getClass().equals(RollingFileAppender.class)) {
                FileReader reader = new FileReader(((RollingFileAppender<ILoggingEvent>) logEvent).getFile());
                BufferedReader bufferedReader = new BufferedReader(reader);
                List<Map<String, Object>> parsedLogLines = this.parseAppenderFile(bufferedReader, startTime, endTime);
                if(!parsedLogLines.isEmpty())
                    res.addAll(parsedLogLines);
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
    private List<Map<String, Object>> parseAppenderFile(BufferedReader bufferedReader, long startTime, long endTime) throws ParseException, IOException {
        List<Map<String, Object>> res = new ArrayList<>();
        String currentLine;
        while((currentLine=bufferedReader.readLine()) != null) {

            @SuppressWarnings("unchecked")
            Map<String,Object> logLine = mapper.readValue(currentLine, Map.class);

            /* Grab the log entry as a map, and check to see if the timestamp falls within range of startTime and endTime */
            if(logLine != null && logLine.containsKey(TIMESTAMP_LOG_KEY)) {
                SimpleDateFormat timestampFormat = new SimpleDateFormat(TIMESTAMP_FORMAT);
                long logTime = timestampFormat.parse(logLine.get(TIMESTAMP_LOG_KEY).toString()).toInstant().toEpochMilli();
                if(logTime > startTime && logTime < endTime) {
                    res.add(logLine);
                }
            }
        }
        return res;
    }
}
