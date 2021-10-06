package com.networknt.logging.handler;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    static final long DEFAULT_MAX_RANGE = Long.MAX_VALUE;
    static final long DEFAULT_MIN_RANGE = Long.MIN_VALUE;

    private static final ObjectMapper mapper = Config.getInstance().getMapper();

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        long requestTimeRangeStart = DEFAULT_MIN_RANGE;
        long requestTimeRangeEnd = DEFAULT_MAX_RANGE;

        LoggerConfig config = (LoggerConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, LoggerConfig.class);
        Map<String, Deque<String>> parameters = exchange.getQueryParameters();

        if (config.isEnabled()) {

            if(parameters.get("startTime").getFirst() != null)
                requestTimeRangeStart = Long.parseLong(parameters.get("startTime").getFirst());
            if(parameters.get("endTime").getFirst() != null)
                requestTimeRangeEnd = Long.parseLong(parameters.get("endTime").getFirst());

            this.getLogEntries(requestTimeRangeStart, requestTimeRangeEnd, exchange);
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
    private void getLogEntries(long startTime, long endTime, HttpServerExchange exchange) {
        List<String> logContent = new ArrayList<>();

        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (ch.qos.logback.classic.Logger log : lc.getLoggerList()) {

            /* only parse the context if the log is valid */
            if (log.getLevel() != null) {
                String logString = this.parseLogContents(startTime, endTime, log, exchange);
                if(logString == null)
                    return;
                logContent.add(logString);
            }
        }

        try {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, ContentType.APPLICATION_JSON.value());
            exchange.getResponseSender().send(mapper.writeValueAsString(logContent));
        } catch (JsonProcessingException e) {
            logger.error("Failed to map appender list");
            setExchangeStatus(exchange, STATUS_LOGGER_FILE_INVALID);
        }
    }

    /**
     *
     * @param startTime - the request start time range when grabbing log entries
     * @param endTime - the request end time range when grabbing log entries
     * @param log - the log context
     * @param exchange - the HttpServerExchange
     * @return - returns the string response for log entry request.
     */
    private String parseLogContents(long startTime, long endTime, ch.qos.logback.classic.Logger log, HttpServerExchange exchange) {
        StringBuilder res = new StringBuilder();
        for(Iterator<Appender<ILoggingEvent>> it = log.iteratorForAppenders(); it.hasNext();) {
            Appender<ILoggingEvent> logEvent = it.next();
            if(logEvent.getClass().equals(RollingFileAppender.class)) {
                try {
                    FileReader reader = new FileReader(((RollingFileAppender<ILoggingEvent>) logEvent).getFile());
                    BufferedReader bufferedReader = new BufferedReader(reader);
                    res.append(this.parseAppenderFile(bufferedReader, startTime, endTime));
                } catch(IOException | ParseException e) {
                    logger.error("Failed to get log file");
                    setExchangeStatus(exchange, STATUS_LOGGER_FILE_INVALID);
                    return null;
                }
            }
        }
        return res.toString();
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
    private String parseAppenderFile(BufferedReader bufferedReader, long startTime, long endTime) throws ParseException, IOException {
        StringBuilder res = new StringBuilder();
        String currentLine;
        while((currentLine=bufferedReader.readLine()) != null) {

            @SuppressWarnings("unchecked")
            Map<String,Object> logLine = mapper.readValue(currentLine, Map.class);

            /* Grab the log entry as a map, and check to see if the timestamp falls within range of startTime and endTime */
            if(logLine != null && logLine.containsKey(TIMESTAMP_LOG_KEY)) {
                SimpleDateFormat timestampFormat = new SimpleDateFormat(TIMESTAMP_FORMAT);
                long logTime = timestampFormat.parse(logLine.get(TIMESTAMP_LOG_KEY).toString()).toInstant().toEpochMilli();
                if(logTime > startTime && logTime < endTime) {
                    res.append(currentLine);
                    res.append("\n");
                }
            }
        }
        return res.toString();
    }
}
