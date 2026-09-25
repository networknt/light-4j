package com.networknt.restrans;

import com.networknt.httpstring.AttachmentConstants;
import com.networknt.handler.BuffersUtils;
import com.networknt.rule.RuleConstants;
import com.networknt.rule.RuleEngine;
import com.networknt.rule.RuleExecutor;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;
import io.undertow.util.Headers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;

class ResponseTransformerRuleTest {

    @Test
    void testResponseTransformSkipsRulesRemovedDuringReload() throws Exception {
        String serviceEntry = "/v1/notifications@get";
        RuleExecutor originalExecutor = SingletonServiceFactory.getBean(RuleExecutor.class);
        RuleExecutor executor = Mockito.mock(RuleExecutor.class);
        Mockito.when(executor.getEndpointRules()).thenReturn(Map.of(serviceEntry,
                Map.of("res-tra", List.of(Map.of("ruleId", "removed-rule")))));
        // A config reload can remove the rule before executeRules reads it again.
        Mockito.when(executor.executeRules(eq(serviceEntry), eq("res-tra"), anyMap())).thenReturn(null);

        DefaultByteBufferPool pool = new DefaultByteBufferPool(false, 1024);
        PooledByteBuffer buffer = pool.allocate();
        buffer.getBuffer().put("original".getBytes(StandardCharsets.UTF_8)).flip();
        HttpServerExchange exchange = new HttpServerExchange(null);
        exchange.setRequestMethod(Methods.GET);
        exchange.setRequestURI("/v1/notifications");
        exchange.setRequestPath("/v1/notifications");
        exchange.getRequestHeaders().put(Headers.HOST, "localhost");
        exchange.putAttachment(AttachmentConstants.BUFFERED_RESPONSE_DATA_KEY, new PooledByteBuffer[]{buffer});

        try {
            SingletonServiceFactory.setBean(RuleExecutor.class.getName(), executor);
            new ResponseTransformerInterceptor().handleRequest(exchange);

            Mockito.verify(executor).executeRules(eq(serviceEntry), eq("res-tra"), anyMap());
            Assertions.assertEquals("original", BuffersUtils.toString(new PooledByteBuffer[]{buffer}, StandardCharsets.UTF_8));
            Assertions.assertEquals(200, exchange.getStatusCode());
        } finally {
            SingletonServiceFactory.setBean(RuleExecutor.class.getName(), originalExecutor);
            buffer.close();
            pool.close();
        }
    }

    @Test
    void testResponseFilterAcceptsRuleIdMapping() throws Exception {
        String serviceEntry = "/v1/notifications@get";
        RuleExecutor originalExecutor = SingletonServiceFactory.getBean(RuleExecutor.class);
        RuleExecutor executor = Mockito.mock(RuleExecutor.class);
        RuleEngine engine = Mockito.mock(RuleEngine.class);
        Mockito.when(executor.getEndpointRules()).thenReturn(Map.of(serviceEntry,
                Map.of("res-fil", List.of(Map.of("ruleId", "filter-response")))));
        Mockito.when(executor.getRuleEngine()).thenReturn(engine);
        Mockito.when(engine.executeRule(eq("filter-response"), anyMap()))
                .thenReturn(Map.of(RuleConstants.RESULT, true, "responseBody", "filtered"));

        DefaultByteBufferPool pool = new DefaultByteBufferPool(false, 1024);
        PooledByteBuffer buffer = pool.allocate();
        buffer.getBuffer().put("original".getBytes(StandardCharsets.UTF_8)).flip();
        HttpServerExchange exchange = new HttpServerExchange(null);
        exchange.setRequestMethod(Methods.GET);
        exchange.setRequestURI("/v1/notifications");
        exchange.setRequestPath("/v1/notifications");
        exchange.getRequestHeaders().put(Headers.HOST, "localhost");
        exchange.putAttachment(AttachmentConstants.BUFFERED_RESPONSE_DATA_KEY, new PooledByteBuffer[]{buffer});

        try {
            SingletonServiceFactory.setBean(RuleExecutor.class.getName(), executor);
            new ResponseFilterInterceptor().handleRequest(exchange);
            Mockito.verify(engine).executeRule(eq("filter-response"), anyMap());
        } finally {
            SingletonServiceFactory.setBean(RuleExecutor.class.getName(), originalExecutor);
            buffer.close();
            pool.close();
        }
    }

    @Test
    void testResponseTransformUsesNormalizedRuleIds() throws Exception {
        String serviceEntry = "/v1/notifications@get";
        RuleExecutor originalExecutor = SingletonServiceFactory.getBean(RuleExecutor.class);
        RuleExecutor executor = Mockito.mock(RuleExecutor.class);
        Mockito.when(executor.getEndpointRules()).thenReturn(Map.of(serviceEntry,
                Map.of("res-tra", List.of(Map.of("ruleId", "json2soap-transformer-response")))));
        Mockito.when(executor.executeRules(eq(serviceEntry), eq("res-tra"), anyMap()))
                .thenReturn(Map.of(RuleConstants.RESULT, true));

        DefaultByteBufferPool pool = new DefaultByteBufferPool(false, 1024);
        PooledByteBuffer buffer = pool.allocate();
        buffer.getBuffer().put("original".getBytes(StandardCharsets.UTF_8)).flip();
        HttpServerExchange exchange = new HttpServerExchange(null);
        exchange.setRequestMethod(Methods.GET);
        exchange.setRequestURI("/v1/notifications");
        exchange.setRequestPath("/v1/notifications");
        exchange.getRequestHeaders().put(Headers.HOST, "localhost");
        exchange.putAttachment(AttachmentConstants.BUFFERED_RESPONSE_DATA_KEY, new PooledByteBuffer[]{buffer});

        try {
            SingletonServiceFactory.setBean(RuleExecutor.class.getName(), executor);
            new ResponseTransformerInterceptor().handleRequest(exchange);

            ArgumentCaptor<Map<String, Object>> input = ArgumentCaptor.forClass(Map.class);
            Mockito.verify(executor).executeRules(eq(serviceEntry), eq("res-tra"), input.capture());
            Assertions.assertEquals("original", input.getValue().get("responseBody"));
        } finally {
            SingletonServiceFactory.setBean(RuleExecutor.class.getName(), originalExecutor);
            buffer.close();
            pool.close();
        }
    }
}
