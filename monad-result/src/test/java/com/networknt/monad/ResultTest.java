package com.networknt.monad;

import com.networknt.status.Status;
import org.junit.Assert;
import org.junit.Test;

import static com.networknt.monad.Success.SUCCESS;

public class ResultTest {
    @Test
    public void testResult() {
        Status status = new Status(400, "ERR00000", "DEMO_STATUS", "This is an error", "ERROR");
        Assert.assertTrue(SUCCESS.isSuccess());
        Assert.assertTrue(!Failure.of(status).isSuccess());
        Assert.assertTrue(!SUCCESS.isFailure());
        Assert.assertTrue(Failure.of(status).isFailure());

        Result result = Failure.of(status);
        Assert.assertTrue(result.getError().equals(status));

        String stringResult = "String result";
        result = Success.of(stringResult);
        Assert.assertEquals(stringResult, result.getResult());
   }
}
