package com.networknt.rule;

import com.networknt.utility.Constants;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class RoleBasedAccessControlActionTest {

    @Test
    void shouldNotMatchRoleBySubstring() throws Exception {
        RoleBasedAccessControlAction action = new RoleBasedAccessControlAction();
        Map<String, Object> objMap = createObjMap("admin user", "host-admin, org-admin");
        Map<String, Object> resultMap = new HashMap<>();

        action.performAction("ruleId", "actionId", objMap, resultMap, List.of());

        Assertions.assertEquals(Boolean.FALSE, resultMap.get(RuleConstants.RESULT));
    }

    @Test
    void shouldMatchExactRole() throws Exception {
        RoleBasedAccessControlAction action = new RoleBasedAccessControlAction();
        Map<String, Object> objMap = createObjMap("admin user", "host-admin, user");
        Map<String, Object> resultMap = new HashMap<>();

        action.performAction("ruleId", "actionId", objMap, resultMap, List.of());

        Assertions.assertEquals(Boolean.TRUE, resultMap.get(RuleConstants.RESULT));
    }

    private Map<String, Object> createObjMap(String jwtRole, String endpointRoles) {
        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setClaim(Constants.ROLE, jwtRole);

        Map<String, Object> auditInfo = new HashMap<>();
        auditInfo.put(Constants.SUBJECT_CLAIMS, jwtClaims);

        Map<String, Object> objMap = new HashMap<>();
        objMap.put(Constants.AUDIT_INFO, auditInfo);
        objMap.put(Constants.ROLES, endpointRoles);
        return objMap;
    }
}
