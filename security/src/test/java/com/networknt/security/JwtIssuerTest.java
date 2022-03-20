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
        JwtClaims claims = ClaimsUtil.getTestClaims("Steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("api_a.w", "api_b.w", "api_c.w", "api_d.w", "server.info.r"), "user");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***LongLived APIA JWT***: " + jwt);
    }

    @Test
    public void longLivedATMP1000Jwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("eric", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("ATMP1000.w", "ATMP1000.r"), "user");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***LongLived ATMP1000 JWT***: " + jwt);
    }


    @Test
    public void longLivedPetStoreJwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("write:pets", "read:pets"), "user");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***LongLived PetStore JWT***: " + jwt);
    }

    @Test
    public void longLivedTrainingJwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("training.accounts.read", "training.customers.read", "training.myaccounts.read", "training.transacts.read"), "user");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***LongLived Training JWT***: " + jwt);
    }

    @Test
    public void longLivedHelloWorldJwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("world.r", "world.w", "server.info.r"), "user");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***LongLived HelloWorld JWT***: " + jwt);
    }

    @Test
    public void longLivedCodegenJwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("codegen.r", "codegen.w", "server.info.r"), "user");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***LongLived Codegen JWT***: " + jwt);
    }

    @Test
    public void longLivedReferenceJwt() throws Exception {
        Map<String, String> custom = new HashMap<>();
        custom.put("consumer_application_id", "361");
        custom.put("request_transit", "67");
        JwtClaims claims = ClaimsUtil.getCustomClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("party.util.reference.read", "server.info.r"), custom, "user admin");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***LongLived reference JWT***: " + jwt);
    }

    @Test
    public void longLivedProductSubjectJwt() throws Exception {
        Map<String, String> custom = new HashMap<>();
        custom.put("consumer_application_id", "361");
        custom.put("request_transit", "67");
        JwtClaims claims = ClaimsUtil.getCustomClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", null, custom, "user");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***LongLived product subject JWT***: " + jwt);
    }

    @Test
    public void longLivedProductAccessJwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("party.product.read", "server.info.r"), "user");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***LongLived product access JWT***: " + jwt);
    }

    @Test
    public void normalPetStoreJwtWithManagerTeller() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("write:pets", "read:pets"), "manager teller");
        claims.setExpirationTimeMinutesInTheFuture(10);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***JWT***: " + jwt);
    }

    @Test
    public void normalPetStoreJwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("write:pets", "read:pets"), "user");
        claims.setExpirationTimeMinutesInTheFuture(10);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***JWT***: " + jwt);
    }

    @Test
    public void longlivedTransferJwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("etransfer.r", "etransfer.w"), "user");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token for etransfer***: " + jwt);
    }

    @Test
    public void longlivedTokenizationJwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("token.r", "token.w", "scheme.r"), "user");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token for tokenizaiton***: " + jwt);
    }

    @Test
    public void longlivedTokenizationJwt73() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e73", Arrays.asList("token.r", "token.w"), "user");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token for tokenizaiton***: " + jwt);
    }

    @Test
    public void longlivedLightPortalLocalhost() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("stevehu@gmail.com", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e73", Arrays.asList("portal.r", "portal.w"), "user lightapi.net admin");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token for portal localhost***: " + jwt);
    }

    @Test
    public void longlivedLightPortalController() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("stevehu@gmail.com", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e73", Arrays.asList("portal.r", "portal.w"), "user CtlPltAdmin CtlPltRead CtlPltWrite");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token for portal controller ***: " + jwt);
    }

    @Test
    public void longlivedPortalAdmin() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("stevehu@lightapi.net", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e73", Arrays.asList("portal.r", "portal.w"), "user admin CtlPltAdmin CtlPltRead CtlPltWrite");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token for portal admin ***: " + jwt);
    }

    @Test
    public void longlivedLightPortalConfigServer() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("stevehu@gmail.com", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e73", Arrays.asList("portal.r", "portal.w"), "user CfgPltAdmin CfgPltRead CfgPltWrite");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token for portal config server ***: " + jwt);
    }


    @Test
    public void longlivedLightPortalLightapi() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("stevehu@gmail.com", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("portal.r", "portal.w"), "user lightapi.net admin");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token for portal lightapi***: " + jwt);
    }

    @Test
    public void longlivedCcLocalPortal() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestCcClaims("f7d42348-c647-4efb-a52d-4c5787421e73", Arrays.asList("portal.r", "portal.w"));
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token for portal lightapi***: " + jwt);
    }

    /**
     * The returned token contains scp as the key for the scope. Some OAuth 2.0 provider like Okta use this claim. All scopes are separated by comma.
     * @throws Exception
     */
    @Test
    public void longlivedCcLocalPortalWithScp() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestCcClaimsWithScp("f7d42348-c647-4efb-a52d-4c5787421e73", Arrays.asList("portal.r", "portal.w"));
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token for portal lightapi***: " + jwt);
    }

    /**
     * The returned token contains scope as the key for the scope. All scopes are separated by space.
     * @throws Exception
     */
    @Test
    public void longlivedCcLocalPortalScope() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestCcClaimsScope("f7d42348-c647-4efb-a52d-4c5787421e73", "portal.r portal.w");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token for portal lightapi***: " + jwt);
    }

    /**
     * The returned token contains scope as the key for the scope. All scopes are separated by space.
     * @throws Exception
     */
    @Test
    public void longlivedCcLocalAdminScope() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestCcClaimsScope("f7d42348-c647-4efb-a52d-4c5787421e73", "admin");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token for admin endpoints***: " + jwt);
    }

    /**
     * The returned token contains scp as the key for the scope. Some OAuth 2.0 provider like Okta use this claim. All scopes are separated by comma.
     * The token will have proxy.r and proxy.w as scopes for testing with proxy configuration in light-config-test proxy folder.
     * @throws Exception
     */
    @Test
    public void longlivedCcLocalProxyWithScp() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestCcClaimsWithScp("f7d42348-c647-4efb-a52d-4c5787421e73", Arrays.asList("proxy.r", "proxy.w"));
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token for proxy***: " + jwt);
    }

    /**
     * This token is used to connect to the light-config-server with serviceId 0100 for testing with a service specific for a client.
     * @throws Exception
     */
    @Test
    public void sidecarReferenceBootstrap() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestCcClaimsScopeService("f7d42348-c647-4efb-a52d-4c5787421e72", "portal.r portal.w", "0100");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Reference Long lived Bootstrap token for config server and controller: " + jwt);
    }

    /**
     * This token is used to connect to the light-config-server with serviceId 0100 for testing with a service specific for a client.
     * @throws Exception
     */
    @Test
    public void petstoreBootstrap() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestCcClaimsScopeService("f7d42348-c647-4efb-a52d-4c5787421e72", "portal.r portal.w", "com.networknt.petstore-3.0.1");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Reference Long lived Bootstrap token for config server and controller: " + jwt);
    }

    /**
     * This token is used to connect to the light-config-server with serviceId 0100 for testing with a service specific for a client.
     * @throws Exception
     */
    @Test
    public void sidecarReferenceBootstrapWithServiceId() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestCcClaimsScopeService("f7d42348-c647-4efb-a52d-4c5787421e72", "A8E73740C0041C03D67C3A951AA1D7533C8F9F2FB57D7BA107210B9BC9E06DA2", "com.networknt.petstore-1.0.0");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Reference Long lived Bootstrap token for config server and controller: " + jwt);
    }

    /**
     * This token is used to connect to the light-config-server with serviceId example-service for unit test populated configs.
     * @throws Exception
     */
    @Test
    public void sidecarExampleBootstrap() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestCcClaimsScopeService("f7d42348-c647-4efb-a52d-4c5787421e72", "portal.r portal.w", "example-service");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Reference Long lived Bootstrap token for config server and controller: " + jwt);
    }

    /**
     * The returned token contains scopes for access-control example
     * @throws Exception
     */
    @Test
    public void CcAccessControl() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestCcClaimsScope("f7d42348-c647-4efb-a52d-4c5787421e73", "account.r account.w");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token for Client Credentials Access Control***: " + jwt);
    }

    /**
     * The returned token contains scopes for access-control example
     * @throws Exception
     */
    @Test
    public void AcRoleAccessControlRight() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("stevehu", "CUSTOMER", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("account.r", "account.w"), "customer");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token Authorization code customer with  roles***: " + jwt);
    }

    /**
     * The returned token contains scopes for access-control example
     * @throws Exception
     */
    @Test
    public void AcRoleAccessControlWrong() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("stevehu", "CUSTOMER", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("account.r", "account.w"), "user");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token Authorization code customer with  roles***: " + jwt);
    }

    /**
     * The returned token contains scopes for access-control example
     * @throws Exception
     */
    @Test
    public void AcGroupAccessControlRight() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaimsGroup("stevehu", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("account.r", "account.w"), "admin frontOffice");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token Authorization code customer with  roles***: " + jwt);
    }

    /**
     * The returned token contains scopes for access-control example
     * @throws Exception
     */
    @Test
    public void AcGroupAccessControlWrong() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaimsGroup("stevehu", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("account.r", "account.w"), "backOffice");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token Authorization code customer with  roles***: " + jwt);
    }

    /**
     * The returned token contains groups User_API_Dev_R User_API_Dev_W for controller-group-role rule
     * @throws Exception
     */
    @Test
    public void GroupToRoleAccessControlRight() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaimsGroup("stevehu", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("portal.r", "portal.w"), "User_API_Dev_R User_API_Dev_W");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token Authorization code customer with controller groups to roles ***: " + jwt);
    }

    /**
     * The returned token contains groups User_API_Wrong for controller-group-role rule
     * @throws Exception
     */
    @Test
    public void GroupToRoleAccessControlWrong() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaimsGroup("stevehu", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("portal.r", "portal.w"), "User_API_Wrong");
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtIssuer.getJwt(claims);
        System.out.println("***Long lived token Authorization code customer with a wrong controller groups that cannot be converted to roles ***: " + jwt);
    }

}
