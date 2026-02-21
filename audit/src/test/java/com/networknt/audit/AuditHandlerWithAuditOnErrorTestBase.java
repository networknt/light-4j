package com.networknt.audit;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuditHandlerWithAuditOnErrorTestBase extends AuditHandlerTestBase {
    private static MockedStatic<Config> mockedConfig;

    @BeforeAll
    public static void init() {
        logger.info("starting server with auditOnError true");

        //mock configuration and set auditOnError to TRUE
        Config originConfig = Config.getInstance();
        Config spyConfig = spy(originConfig);
        Map<String, Object> originAuditConfig = spyConfig.getDefaultJsonMapConfigNoCache("audit");
        originAuditConfig.put("auditOnError", Boolean.TRUE);
        doReturn(originAuditConfig).when(spyConfig).getDefaultJsonMapConfigNoCache(eq("audit"));
        mockedConfig = mockStatic(Config.class);
        mockedConfig.when(Config::getInstance).thenReturn(spyConfig);

        setUp();
    }

    @AfterAll
    public static void close() {
        if (mockedConfig != null) {
            mockedConfig.close();
        }
    }

    @Test
    public void testAuditWithDumpResponse() throws Exception {
        runTest("/error", "post", null, 401);
        verifyAuditInfo("responseBody", "{\"statusCode\":401,\"code\":\"ERR10001\",\"message\":\"AUTH_TOKEN_EXPIRED\",\"description\":\"Jwt token in authorization header expired\",\"severity\":\"ERROR\"}");
    }

    @Test
    public void testAuditWithoutDumpResponse() throws Exception {
        runTest("/pet", "post", null, 200);
        verifyAuditInfo("responseBody", null);
    }

    @Test
    public void testAuditWithErrorStatus() throws Exception {
        runTest("/error", "post", null, 401);
        verifyAuditErrorStatus();
    }

    private void verifyAuditErrorStatus() {
        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
        ILoggingEvent event = captorLoggingEvent.getValue();
        Map<String, Object> mapValue = JsonMapper.string2Map(event.getFormattedMessage());

        Assertions.assertEquals("{statusCode=401, code=ERR10001, severity=ERROR, message=AUTH_TOKEN_EXPIRED, description=Jwt token in authorization header expired}", mapValue.get("Status").toString());
    }
}
