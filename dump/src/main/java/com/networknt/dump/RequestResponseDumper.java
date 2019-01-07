package com.networknt.dump;

import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.networknt.dump.DumpConstants.REQUEST;
import static com.networknt.dump.DumpConstants.RESPONSE;

/**
 * Root dumper to delegate child dumpers dump Requests and Responses
 */
public class RequestResponseDumper extends AbstractDumper {
    private Map<String, Object> httpMethodMap = new LinkedHashMap<>();
    private List<IDumpable> childDumpers;
    private static Logger logger = LoggerFactory.getLogger(RequestResponseDumper.class);
    RequestResponseDumper(Object config, HttpServerExchange exchange, HttpMessageType type) {
        super(config, exchange, type);
        initializeChildDumpers();
    }

    @Override
    protected void loadConfig() {
        if(this.type == HttpMessageType.RESPONSE) {
            //load Response config
            loadEnableConfig(DumpConstants.RESPONSE);
        } else {
            //load Request config
            loadEnableConfig(DumpConstants.REQUEST);
        }
    }

    @Override
    public void dump() {
        if(isApplicable()) {
            delegateToChildDumpers();
        }
    }

    private void delegateToChildDumpers() {
        childDumpers.forEach(dumper -> {
            try{
                dumper.dump();
                dumper.putResultTo(httpMethodMap);
            } catch (Exception e) {
                logger.error(e.toString());
            }
        });
    }

    @Override
    public Map<String, Object> getResult() {
        return this.httpMethodMap;
    }

    @Override
    public void putResultTo(Map<String, Object> result) {
        if(this.httpMethodMap.size() > 0) {
            if(this.type == HttpMessageType.RESPONSE) {
                result.put(RESPONSE, this.httpMethodMap);
            } else {
                result.put(REQUEST, this.httpMethodMap);
            }
        }
    }

    private void initializeChildDumpers() {
        IDumpable bodyDumper = new BodyDumper(config, exchange, type);
        IDumpable cookiesDumper = new CookiesDumper(config, exchange, type);
        IDumpable headersDumper = new HeadersDumper(config, exchange, type);
        IDumpable queryParametersDumper = new QueryParametersDumper(config, exchange, type);
        IDumpable statusCodeDumper = new StatusCodeDumper(config, exchange, type);
        this.childDumpers = new ArrayList<>(Arrays.asList(bodyDumper, cookiesDumper, headersDumper, queryParametersDumper, statusCodeDumper));
    }
}
