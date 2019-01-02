package com.networknt.dump;

import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.networknt.dump.DumpConstants.*;

public class DumperFactory {
    private Logger logger = LoggerFactory.getLogger(DumperFactory.class);
    private List<String> requestDumpers = Arrays.asList(BODY, COOKIES, HEADERS, QUERY_PARAMETERS, URL);
    private List<String> responseDumpers = Arrays.asList(BODY, COOKIES, HEADERS, STATUS_CODE);

    public List<IRequestDumpable> createRequestDumpers(Object requestConfig, HttpServerExchange exchange, boolean maskEnabled) {

        RequestDumperFactory factory = new RequestDumperFactory();
        List<IRequestDumpable> dumpers = new ArrayList<>();
        for(String dumperNames: requestDumpers) {
            IRequestDumpable dumper = factory.create(dumperNames, requestConfig, exchange, maskEnabled);
            dumpers.add(dumper);
        }
        return dumpers;
    }

    public List<IResponseDumpable> createResponseDumpers(Object responseConfig, HttpServerExchange exchange, boolean maskEnabled) {
        ResponseDumperFactory factory = new ResponseDumperFactory();
        List<IResponseDumpable> dumpers = new ArrayList<>();
        for(String dumperNames: responseDumpers) {
            IResponseDumpable dumper = factory.create(dumperNames, responseConfig, exchange, maskEnabled);
            dumpers.add(dumper);
        }
        return dumpers;
    }

    public class RequestDumperFactory{
        public IRequestDumpable create(String dumperName, Object config, HttpServerExchange exchange, boolean maskEnabled) {
            switch (dumperName) {
                case DumpConstants.BODY:
                    return new BodyDumper(config, exchange, maskEnabled);
                case  DumpConstants.COOKIES:
                    return new CookiesDumper(config, exchange, maskEnabled);
                case DumpConstants.HEADERS:
                    return new HeadersDumper(config, exchange, maskEnabled);
                case DumpConstants.QUERY_PARAMETERS:
                    return new QueryParametersDumper(config, exchange, maskEnabled);
                case DumpConstants.URL:
                    return new UrlDumper(config, exchange, maskEnabled);
                default:
                    logger.error("unsupported dump type: {}", dumperName);
                    return null;
            }
        }
    }

    public class ResponseDumperFactory{
        public IResponseDumpable create(String dumperName, Object config, HttpServerExchange exchange, boolean maskEnabled) {
            switch (dumperName) {
                case DumpConstants.BODY:
                    return new BodyDumper(config, exchange, maskEnabled);
                case  DumpConstants.COOKIES:
                    return new CookiesDumper(config, exchange, maskEnabled);
                case DumpConstants.HEADERS:
                    return new HeadersDumper(config, exchange, maskEnabled);
                case DumpConstants.STATUS_CODE:
                    return new StatusCodeDumper(config, exchange, maskEnabled);
                default:
                    logger.error("unsupported dump type: {}", dumperName);
                    return null;
            }
        }
    }

}
