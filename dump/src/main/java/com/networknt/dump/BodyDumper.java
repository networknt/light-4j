package com.networknt.dump;

import com.networknt.utility.StringUtils;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class BodyDumper extends AbstractDumper implements IRequestDumpable, IResponseDumpable{
    private static final Logger logger = LoggerFactory.getLogger(BodyDumper.class);
    private String bodyContent = "";

    BodyDumper(Object config, HttpServerExchange exchange) {
        super(config, exchange);
    }

    @Override
    public void putDumpInfoTo(Map<String, Object> result) {
        if(StringUtils.isNotBlank(this.bodyContent)) {
            result.put(DumpConstants.BODY, this.bodyContent);
        }
    }

    @Override
    protected void loadConfig() {
        loadEnableConfig(DumpConstants.BODY);
    }

    @Override
    public void dumpRequest(Map<String, Object> result) {
        if(!isEnabled()) {
            return;
        }
        //dump request body
        exchange.startBlocking();
        String body = "";
        InputStream inputStream = exchange.getInputStream();
        try {
            body = StringUtils.inputStreamToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error(e.toString());
        }
        this.bodyContent = body;
        this.putDumpInfoTo(result);
    }


    @Override
    public void dumpResponse(Map<String, Object> result) {
        if(!isEnabled()) {
            return;
        }
        byte[] responseBodyAttachment = exchange.getAttachment(StoreResponseStreamSinkConduit.RESPONSE);
        if(responseBodyAttachment != null) {
            this.bodyContent = new String(responseBodyAttachment);
        }
        this.putDumpInfoTo(result);
    }
}
