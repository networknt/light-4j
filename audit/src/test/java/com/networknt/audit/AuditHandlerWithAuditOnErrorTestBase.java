package com.networknt.audit;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Config.class})
@PowerMockIgnore({"javax.*", "org.xml.sax.*", "org.apache.log4j.*", "java.xml.*", "com.sun.*"})
public class AuditHandlerWithAuditOnErrorTestBase extends AuditHandlerTestBase {
    @BeforeClass
    public static void init() {
        logger.info("starting server with auditOnError true");

        //mock configuration and set auditOnError to TRUE
        Config originConfig = Config.getInstance();
        Config spyConfig = spy(originConfig);
        Map<String, Object> originAuditConfig = spyConfig.getDefaultJsonMapConfigNoCache("audit");
        originAuditConfig.put("auditOnError", Boolean.TRUE);
        doReturn(originAuditConfig).when(spyConfig).getDefaultJsonMapConfigNoCache(eq("audit"));
        PowerMockito.mockStatic(Config.class);
        PowerMockito.when(Config.getInstance()).thenReturn(spyConfig);

        setUp();
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

        Assert.assertEquals("{statusCode=401, code=ERR10001, severity=ERROR, message=AUTH_TOKEN_EXPIRED, description=Jwt token in authorization header expired}", mapValue.get("Status").toString());
    }
}
