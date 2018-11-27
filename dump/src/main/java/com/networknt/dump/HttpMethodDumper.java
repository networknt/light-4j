package com.networknt.dump;

import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.networknt.dump.DumpConstants.*;

public class HttpMethodDumper implements IDumpable {
    private static final Logger logger = LoggerFactory.getLogger(HttpMethodDumper.class);
    private Map<String, Object> httpMessageMap = new LinkedHashMap<>();

    private HttpMessageType type;
    private HttpServerExchange exchange;

    HttpMethodDumper(HttpMessageType type, HttpServerExchange exchange) {
        this.type = type;
        this.exchange = exchange;
    }

    @Override
    public void dumpOption(Boolean configObject) {
        if(configObject){
            for(String requestOption: DumpHelper.getSupportHttpMessageOptions(type)) {
                //if request/response option is true, put all supported request child options with true
                dumpHttpMethodBasedOnOptionName(requestOption, httpMessageMap, exchange, true, type);
            }
        }
    }

    @Override
    public void dumpOption(Map configObject) {
        String[] configOptions = type == HttpMessageType.RESPONSE ? RESPONSE_OPTIONS : REQUEST_OPTIONS;
        for(String requestOrResponseOption: configOptions) {
            if(configObject.containsKey(requestOrResponseOption)) {
                dumpHttpMethodBasedOnOptionName(requestOrResponseOption, httpMessageMap, exchange, configObject.get(requestOrResponseOption), type);
            }
        }

    }

    @Override
    public Map<String, Object> getResult() {
        return this.httpMessageMap;
    }

    //Based on option name inside "request" or "response", call related handle method. e.g.  "cookies: true" inside "header", will call "dumpCookie"
    private void dumpHttpMethodBasedOnOptionName(String httpMessageOption, Map<String, Object> result, HttpServerExchange exchange, Object configObject, IDumpable.HttpMessageType type) {
        String composedHttpMessageOptionMethodName = DUMP_METHOD_PREFIX + httpMessageOption.substring(0, 1).toUpperCase() + httpMessageOption.substring(1);
        try {
            Method dumpHttpMessageOptionMethod = DumpHandler.class.getDeclaredMethod(composedHttpMessageOptionMethodName, Map.class, HttpServerExchange.class, Object.class, IDumpable.HttpMessageType.class);
            dumpHttpMessageOptionMethod.invoke(this, result, exchange, configObject, type);
        } catch (NoSuchMethodException e) {
            logger.error("Cannot find a method for this request option: {}", httpMessageOption);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
