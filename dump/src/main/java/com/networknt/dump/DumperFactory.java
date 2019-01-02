package com.networknt.dump;

import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.networknt.dump.DumpConstants.*;

class DumperFactory {
    private Logger logger = LoggerFactory.getLogger(DumperFactory.class);
    private List<String> requestDumpers = Arrays.asList(BODY, COOKIES, HEADERS, QUERY_PARAMETERS, URL);
    private List<String> responseDumpers = Arrays.asList(BODY, COOKIES, HEADERS, STATUS_CODE);

    public List<IRequestDumpable> createRequestDumpers(DumpConfig config, HttpServerExchange exchange) {

        RequestDumperFactory factory = new RequestDumperFactory();
        List<IRequestDumpable> dumpers = new ArrayList<>();
        for(String dumperNames: requestDumpers) {
            IRequestDumpable dumper = factory.create(dumperNames, config, exchange);
            dumpers.add(dumper);
        }
        return dumpers;
    }

    public List<IResponseDumpable> createResponseDumpers(DumpConfig config, HttpServerExchange exchange) {
        ResponseDumperFactory factory = new ResponseDumperFactory();
        List<IResponseDumpable> dumpers = new ArrayList<>();
        for(String dumperNames: responseDumpers) {
            IResponseDumpable dumper = factory.create(dumperNames, config, exchange);
            dumpers.add(dumper);
        }
        return dumpers;
    }

    class RequestDumperFactory{
        IRequestDumpable create(String dumperName, DumpConfig config, HttpServerExchange exchange) {
            switch (dumperName) {
                case DumpConstants.BODY:
                    return new BodyDumper(config, exchange);
                case  DumpConstants.COOKIES:
                    return new CookiesDumper(config, exchange);
                case DumpConstants.HEADERS:
                    return new HeadersDumper(config, exchange);
                case DumpConstants.QUERY_PARAMETERS:
                    return new QueryParametersDumper(config, exchange);
                case DumpConstants.URL:
                    return new UrlDumper(config, exchange);
                default:
                    logger.error("unsupported dump type: {}", dumperName);
                    return null;
            }
        }
    }

    class ResponseDumperFactory{
        IResponseDumpable create(String dumperName, DumpConfig config, HttpServerExchange exchange) {
            switch (dumperName) {
                case DumpConstants.BODY:
                    return new BodyDumper(config, exchange);
                case  DumpConstants.COOKIES:
                    return new CookiesDumper(config, exchange);
                case DumpConstants.HEADERS:
                    return new HeadersDumper(config, exchange);
                case DumpConstants.STATUS_CODE:
                    return new StatusCodeDumper(config, exchange);
                default:
                    logger.error("unsupported dump type: {}", dumperName);
                    return null;
            }
        }
    }
}
