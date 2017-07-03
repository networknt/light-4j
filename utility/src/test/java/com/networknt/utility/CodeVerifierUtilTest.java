package com.networknt.utility;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by steve on 22/06/17.
 */
public class CodeVerifierUtilTest {

    @Test
    public void testCodeVerifier() {
        String v = CodeVerifierUtil.generateRandomCodeVerifier();
        System.out.println("v = " + v);
        String c1 = CodeVerifierUtil.deriveCodeVerifierChallenge(v);
        System.out.println("c1 = " + c1);

        String c2 = CodeVerifierUtil.deriveCodeVerifierChallenge(v);
        System.out.println("c2 = " + c2);
        Assert.assertTrue(c1.equals(c2));
    }

    
}
