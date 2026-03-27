package com.networknt.rule;

import com.networknt.utility.Constants;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ResponseFilterActionsTest {

    @Test
    void responseColumnFilterShouldNotMatchRoleBySubstring() throws Exception {
        ResponseColumnFilterAction action = new ResponseColumnFilterAction();
        Map<String, Object> objMap = createFilterObjMap(
                "[{\"id\":1,\"secret\":\"x\"}]",
                createClaims("admin user", null),
                Constants.COL,
                Map.of(Constants.ROLE, Map.of("host-admin", "[\"id\"]")));
        Map<String, Object> resultMap = new HashMap<>();

        action.performAction("ruleId", "actionId", objMap, resultMap, List.of());

        Assertions.assertEquals(Boolean.TRUE, resultMap.get(RuleConstants.RESULT));
        Assertions.assertEquals("[{\"id\":1,\"secret\":\"x\"}]", resultMap.get("responseBody"));
    }

    @Test
    void responseColumnFilterShouldUseUidForUserMatching() throws Exception {
        ResponseColumnFilterAction action = new ResponseColumnFilterAction();
        Map<String, Object> objMap = createFilterObjMap(
                "[{\"id\":1,\"secret\":\"x\"}]",
                createClaims(null, "steve"),
                Constants.COL,
                Map.of(Constants.USER, Map.of("steve", "[\"id\"]")));
        Map<String, Object> resultMap = new HashMap<>();

        action.performAction("ruleId", "actionId", objMap, resultMap, List.of());

        Assertions.assertEquals(Boolean.TRUE, resultMap.get(RuleConstants.RESULT));
        Assertions.assertEquals("[{\"id\":1}]", resultMap.get("responseBody"));
    }

    @Test
    void responseRowFilterShouldNotMatchRoleBySubstring() throws Exception {
        ResponseRowFilterAction action = new ResponseRowFilterAction();
        Map<String, Object> objMap = createFilterObjMap(
                "[{\"status\":\"O\"},{\"status\":\"C\"}]",
                createClaims("admin user", null),
                Constants.ROW,
                Map.of(Constants.ROLE, Map.of("host-admin", List.of(Map.of("colName", "status", "operator", "=", "colValue", "O")))));
        Map<String, Object> resultMap = new HashMap<>();

        action.performAction("ruleId", "actionId", objMap, resultMap, List.of());

        Assertions.assertEquals(Boolean.TRUE, resultMap.get(RuleConstants.RESULT));
        Assertions.assertEquals("[{\"status\":\"O\"},{\"status\":\"C\"}]", resultMap.get("responseBody"));
    }

    @Test
    void responseRowFilterShouldUseUidForUserMatching() throws Exception {
        ResponseRowFilterAction action = new ResponseRowFilterAction();
        Map<String, Object> objMap = createFilterObjMap(
                "[{\"status\":\"O\"},{\"status\":\"C\"}]",
                createClaims(null, "steve"),
                Constants.ROW,
                Map.of(Constants.USER, Map.of("steve", List.of(Map.of("colName", "status", "operator", "=", "colValue", "O")))));
        Map<String, Object> resultMap = new HashMap<>();

        action.performAction("ruleId", "actionId", objMap, resultMap, List.of());

        Assertions.assertEquals(Boolean.TRUE, resultMap.get(RuleConstants.RESULT));
        Assertions.assertEquals("[{\"status\":\"O\"}]", resultMap.get("responseBody"));
    }

    private Map<String, Object> createFilterObjMap(String responseBody, JwtClaims jwtClaims, String filterKey, Map<String, Object> filterConfig) {
        Map<String, Object> auditInfo = new HashMap<>();
        auditInfo.put(Constants.SUBJECT_CLAIMS, jwtClaims);

        Map<String, Object> objMap = new HashMap<>();
        objMap.put("responseBody", responseBody);
        objMap.put(Constants.AUDIT_INFO, auditInfo);
        objMap.put(filterKey, filterConfig);
        return objMap;
    }

    private JwtClaims createClaims(String role, String uid) {
        JwtClaims jwtClaims = new JwtClaims();
        if (role != null) {
            jwtClaims.setClaim(Constants.ROLE, role);
        }
        if (uid != null) {
            jwtClaims.setClaim(Constants.UID, uid);
        }
        return jwtClaims;
    }
}
