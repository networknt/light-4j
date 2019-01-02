package com.networknt.dump;

import com.networknt.body.BodyHandler;
import com.networknt.mask.Mask;
import com.networknt.utility.StringUtils;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class BodyDumper extends AbstractDumper implements IRequestDumpable, IResponseDumpable{
    private static final Logger logger = LoggerFactory.getLogger(BodyDumper.class);
    private String bodyContent = "";

    BodyDumper(Object config, HttpServerExchange exchange, Boolean maskEnabled) {
        super(config, exchange, maskEnabled);
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

        String contentType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
        //only dump json info
        if (contentType != null && contentType.startsWith("application/json")) {
            //if body info already grab by body handler, get it from attachment directly
            Object requestBodyAttachment = exchange.getAttachment(BodyHandler.REQUEST_BODY);
            if(requestBodyAttachment != null) {
                dumpBodyAttachment(requestBodyAttachment);
                //otherwise get it from input stream directly
            } else {
                try{
                    dumpInputStream();
                } catch (IOException e) {
                    logger.error("undertow inputstream error:" + e.getMessage());
                }
            }
        } else {
            logger.info("unsupported contentType: {}", contentType);
        }
        this.putDumpInfoTo(result);
    }

    @Override
    public void dumpResponse(Map<String, Object> result) {
        if(!isEnabled()) {
            return;
        }
        byte[] responseBodyAttachment = exchange.getAttachment(StoreResponseStreamSinkConduit.RESPONSE);
        if(responseBodyAttachment != null) {
            if (isMaskEnabled()) {
                this.bodyContent = Mask.maskJson(new ByteArrayInputStream(responseBodyAttachment), "responseBody");
            } else {
                this.bodyContent = new String(responseBodyAttachment);
            }
        }
        this.putDumpInfoTo(result);
    }

    private void dumpInputStream() throws IOException {
        //dump request body
        exchange.startBlocking();
        String body = "";
        InputStream inputStream = exchange.getInputStream();
        if(isMaskEnabled() && inputStream.available() != -1) {
            this.bodyContent = Mask.maskJson(inputStream, "requestBody");
        } else {
            this.bodyContent = body;
        }
    }

    private void dumpBodyAttachment(Object requestBodyAttachment) {
        if(isMaskEnabled()) {
            this.bodyContent = Mask.maskJson(requestBodyAttachment, "requestBody");
        } else {
            this.bodyContent = requestBodyAttachment.toString();
        }
    }
}
