package com.networknt.status;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SeparateClassloaderTestRunner.class)
public class AppStatusTest {

    @Test
    public void testConstructor() {
        AppStatusStartupHook appStatusStartupHook = new AppStatusStartupHook();
        appStatusStartupHook.onStartup();
        Status status = new Status("ERR99999");
        Assert.assertEquals(404, status.getStatusCode());
    }
}
