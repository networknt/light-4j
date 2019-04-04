/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.security;

import org.jose4j.jwt.JwtClaims;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JwtIssuerTest {
    @Test
    public void longLivedAPIAJwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("Steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("api_a.w", "api_b.w", "api_c.w", "api_d.w", "server.info.r"));
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***LongLived APIA JWT***: " + jwt);
    }

    @Test
    public void longLivedATMP1000Jwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("eric", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("ATMP1000.w", "ATMP1000.r"));
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***LongLived ATMP1000 JWT***: " + jwt);
    }


    @Test
    public void longLivedPetStoreJwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("write:pets", "read:pets"));
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***LongLived PetStore JWT***: " + jwt);
    }

    @Test
    public void longLivedTrainingJwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("training.accounts.read", "training.customers.read", "training.myaccounts.read", "training.transacts.read"));
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***LongLived Training JWT***: " + jwt);
    }

    @Test
    public void longLivedHelloWorldJwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("world.r", "world.w", "server.info.r"));
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***LongLived HelloWorld JWT***: " + jwt);
    }

    @Test
    public void longLivedCodegenJwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("codegen.r", "codegen.w", "server.info.r"));
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***LongLived Codegen JWT***: " + jwt);
    }

    @Test
    public void longLivedReferenceJwt() throws Exception {
        Map<String, String> custom = new HashMap<>();
        custom.put("consumer_application_id", "361");
        custom.put("request_transit", "67");
        JwtClaims claims = ClaimsUtil.getCustomClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("party.util.reference.read", "server.info.r"), custom);
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***LongLived reference JWT***: " + jwt);
    }

    @Test
    public void longLivedProductSubjectJwt() throws Exception {
        Map<String, String> custom = new HashMap<>();
        custom.put("consumer_application_id", "361");
        custom.put("request_transit", "67");
        JwtClaims claims = ClaimsUtil.getCustomClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", null, custom);
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***LongLived product subject JWT***: " + jwt);
    }

    @Test
    public void longLivedProductAccessJwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("party.product.read", "server.info.r"));
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***LongLived product access JWT***: " + jwt);
    }

    @Test
    public void normalPetStoreJwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("write:pets", "read:pets"));
        claims.setExpirationTimeMinutesInTheFuture(10);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***JWT***: " + jwt);
    }

    @Test
    public void longlivedTransferJwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("etransfer.r", "etransfer.w"));
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token for etransfer***: " + jwt);
    }

    @Test
    public void longlivedTokenizationJwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("token.r", "token.w", "scheme.r"));
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token for tokenizaiton***: " + jwt);
    }

    @Test
    public void longlivedTokenizationJwt73() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e73", Arrays.asList("token.r", "token.w"));
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token for tokenizaiton***: " + jwt);
    }

}
