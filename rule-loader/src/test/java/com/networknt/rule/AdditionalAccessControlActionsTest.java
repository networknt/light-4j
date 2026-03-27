package com.networknt.rule;

import com.networknt.utility.Constants;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AdditionalAccessControlActionsTest {

    @Test
    void groupActionShouldNotMatchBySubstring() throws Exception {
        GroupBasedAccessControlAction action = new GroupBasedAccessControlAction();
        Map<String, Object> objMap = createObjMap(Constants.GRP, "ops team", Constants.GROUPS, "devops, qa");

        Map<String, Object> resultMap = new HashMap<>();
        action.performAction("ruleId", "actionId", objMap, resultMap, List.of());

        Assertions.assertEquals(Boolean.FALSE, resultMap.get(RuleConstants.RESULT));
    }

    @Test
    void groupActionShouldMatchExactGroup() throws Exception {
        GroupBasedAccessControlAction action = new GroupBasedAccessControlAction();
        Map<String, Object> objMap = createObjMap(Constants.GRP, "ops team", Constants.GROUPS, "devops, team");

        Map<String, Object> resultMap = new HashMap<>();
        action.performAction("ruleId", "actionId", objMap, resultMap, List.of());

        Assertions.assertEquals(Boolean.TRUE, resultMap.get(RuleConstants.RESULT));
    }

    @Test
    void positionActionShouldNotMatchBySubstring() throws Exception {
        PositionBasedAccessControlAction action = new PositionBasedAccessControlAction();
        Map<String, Object> objMap = createObjMap(Constants.POS, "admin user", Constants.POSITIONS, "host-admin, org-admin");

        Map<String, Object> resultMap = new HashMap<>();
        action.performAction("ruleId", "actionId", objMap, resultMap, List.of());

        Assertions.assertEquals(Boolean.FALSE, resultMap.get(RuleConstants.RESULT));
    }

    @Test
    void positionActionShouldMatchExactPosition() throws Exception {
        PositionBasedAccessControlAction action = new PositionBasedAccessControlAction();
        Map<String, Object> objMap = createObjMap(Constants.POS, "admin user", Constants.POSITIONS, "host-admin, user");

        Map<String, Object> resultMap = new HashMap<>();
        action.performAction("ruleId", "actionId", objMap, resultMap, List.of());

        Assertions.assertEquals(Boolean.TRUE, resultMap.get(RuleConstants.RESULT));
    }

    @Test
    void userActionShouldNotMatchBySubstring() throws Exception {
        UserBasedAccessControlAction action = new UserBasedAccessControlAction();
        Map<String, Object> objMap = createObjMap(Constants.UID, "steve", Constants.USERS, "steve.hu, michael");

        Map<String, Object> resultMap = new HashMap<>();
        action.performAction("ruleId", "actionId", objMap, resultMap, List.of());

        Assertions.assertEquals(Boolean.FALSE, resultMap.get(RuleConstants.RESULT));
    }

    @Test
    void userActionShouldMatchExactUser() throws Exception {
        UserBasedAccessControlAction action = new UserBasedAccessControlAction();
        Map<String, Object> objMap = createObjMap(Constants.UID, "steve", Constants.USERS, "michael, steve");

        Map<String, Object> resultMap = new HashMap<>();
        action.performAction("ruleId", "actionId", objMap, resultMap, List.of());

        Assertions.assertEquals(Boolean.TRUE, resultMap.get(RuleConstants.RESULT));
    }

    @Test
    void attributeActionShouldMatchConfiguredAttributes() throws Exception {
        AttributeBasedAccessControlAction action = new AttributeBasedAccessControlAction();
        Map<String, Object> objMap = createObjMap(
                Constants.ATT,
                "department^=^operations~region^=^ca",
                Constants.ATTRIBUTES,
                List.of(Map.of("department", "operations")));

        Map<String, Object> resultMap = new HashMap<>();
        action.performAction("ruleId", "actionId", objMap, resultMap, List.of());

        Assertions.assertEquals(Boolean.TRUE, resultMap.get(RuleConstants.RESULT));
    }

    @Test
    void attributeActionShouldReturnFalseWhenConfiguredAttributesDoNotMatch() throws Exception {
        AttributeBasedAccessControlAction action = new AttributeBasedAccessControlAction();
        Map<String, Object> objMap = createObjMap(
                Constants.ATT,
                "department^=^operations~region^=^ca",
                Constants.ATTRIBUTES,
                List.of(Map.of("department", "finance")));

        Map<String, Object> resultMap = new HashMap<>();
        action.performAction("ruleId", "actionId", objMap, resultMap, List.of());

        Assertions.assertEquals(Boolean.FALSE, resultMap.get(RuleConstants.RESULT));
    }

    private Map<String, Object> createObjMap(String claimName, Object claimValue, String configName, Object configValue) {
        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setClaim(claimName, claimValue);

        Map<String, Object> auditInfo = new HashMap<>();
        auditInfo.put(Constants.SUBJECT_CLAIMS, jwtClaims);

        Map<String, Object> objMap = new HashMap<>();
        objMap.put(Constants.AUDIT_INFO, auditInfo);
        objMap.put(configName, configValue);
        return objMap;
    }
}
