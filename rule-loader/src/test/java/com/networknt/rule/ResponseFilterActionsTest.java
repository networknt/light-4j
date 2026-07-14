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

    @Test
    void responseRowFilterShouldDenyRowsForUnknownStringOperator() throws Exception {
        assertRoleRowFilter(
                "[{\"status\":\"O\"}]",
                List.of(Map.of("colName", "status", "operator", "==", "colValue", "O")),
                "[]");
    }

    @Test
    void responseRowFilterShouldDenyRowsForUnknownNumericOperator() throws Exception {
        assertRoleRowFilter(
                "[{\"priority\":50}]",
                List.of(Map.of("colName", "priority", "operator", "approximately", "colValue", "50")),
                "[]");
    }

    @Test
    void responseRowFilterShouldApplySupportedNumericOperator() throws Exception {
        assertRoleRowFilter(
                "[{\"priority\":25},{\"priority\":75}]",
                List.of(Map.of("colName", "priority", "operator", "<", "colValue", "50")),
                "[{\"priority\":25}]");
    }

    @Test
    void responseRowFilterShouldApplyNumericRangeOperator() throws Exception {
        assertRoleRowFilter(
                "[{\"priority\":24},{\"priority\":25},{\"priority\":75},{\"priority\":76}]",
                List.of(Map.of("colName", "priority", "operator", "range", "colValue", "[25, 75]")),
                "[{\"priority\":25},{\"priority\":75}]");
    }

    @Test
    void responseRowFilterShouldAcceptSingleTokenInValue() throws Exception {
        assertRoleRowFilter(
                "[{\"status\":\"O\"},{\"status\":\"C\"}]",
                List.of(Map.of("colName", "status", "operator", "in", "colValue", "O")),
                "[{\"status\":\"O\"}]");
    }

    @Test
    void responseRowFilterShouldPreserveSpacesInListValues() throws Exception {
        String responseBody = "[{\"region\":\"New York\"},{\"region\":\"CA\"},{\"region\":\"New\"}]";
        String expectedBody = "[{\"region\":\"New York\"},{\"region\":\"CA\"}]";

        assertRoleRowFilter(
                responseBody,
                List.of(Map.of("colName", "region", "operator", "in", "colValue", "[\"New York\", \"CA\"]")),
                expectedBody);
        assertRoleRowFilter(
                responseBody,
                List.of(Map.of("colName", "region", "operator", "in", "colValue", "[New York, CA]")),
                expectedBody);
    }

    @Test
    void responseRowFilterShouldDenyRowsWithMissingColumns() throws Exception {
        assertRoleRowFilter(
                "[{\"other\":\"O\"}]",
                List.of(Map.of("colName", "status", "operator", "=", "colValue", "O")),
                "[]");
    }

    @Test
    void responseRowFilterShouldDenyMalformedFilters() throws Exception {
        assertRoleRowFilter(
                "[{\"status\":\"O\"}]",
                List.of(Map.of("operator", "=", "colValue", "O")),
                "[]");
        assertRoleRowFilter(
                "[{\"status\":\"O\"}]",
                List.of(),
                "[]");
        assertRoleRowFilter(
                "[{\"status\":\"O\"}]",
                List.of(Map.of("colName", "status", "operator", "=", "colValue", Map.of("value", "O"))),
                "[]");
    }

    @Test
    void responseRowFilterShouldRequireEveryMatchedPermissionGroup() throws Exception {
        ResponseRowFilterAction action = new ResponseRowFilterAction();
        Map<String, Object> objMap = createFilterObjMap(
                "[{\"status\":\"O\",\"region\":\"CA\"},{\"status\":\"O\",\"region\":\"US\"},{\"status\":\"C\",\"region\":\"CA\"}]",
                createClaims("teller manager", null),
                Constants.ROW,
                Map.of(Constants.ROLE, Map.of(
                        "teller", List.of(Map.of("colName", "status", "operator", "=", "colValue", "O")),
                        "manager", List.of(Map.of("colName", "region", "operator", "=", "colValue", "CA")))));
        Map<String, Object> resultMap = new HashMap<>();

        action.performAction("ruleId", "actionId", objMap, resultMap, List.of());

        Assertions.assertEquals(Boolean.TRUE, resultMap.get(RuleConstants.RESULT));
        Assertions.assertEquals("[{\"status\":\"O\",\"region\":\"CA\"}]", resultMap.get("responseBody"));
    }

    @Test
    void responseRowFilterShouldDenyWhenAnyMatchedPermissionGroupIsInvalid() throws Exception {
        ResponseRowFilterAction action = new ResponseRowFilterAction();
        Map<String, Object> objMap = createFilterObjMap(
                "[{\"region\":\"CA\"}]",
                createClaims("teller manager", null),
                Constants.ROW,
                Map.of(Constants.ROLE, Map.of(
                        "teller", List.of(),
                        "manager", List.of(Map.of("colName", "region", "operator", "=", "colValue", "CA")))));
        Map<String, Object> resultMap = new HashMap<>();

        action.performAction("ruleId", "actionId", objMap, resultMap, List.of());

        Assertions.assertEquals(Boolean.TRUE, resultMap.get(RuleConstants.RESULT));
        Assertions.assertEquals("[]", resultMap.get("responseBody"));
    }

    @Test
    void responseRowFilterShouldDropNonObjectRows() throws Exception {
        assertRoleRowFilter(
                "[{\"status\":\"O\"},\"O\"]",
                List.of(Map.of("colName", "status", "operator", "=", "colValue", "O")),
                "[{\"status\":\"O\"}]");
    }

    @Test
    void responseRowFilterShouldDenyMissingClaimReferences() throws Exception {
        assertRoleRowFilter(
                "[{\"region\":\"CA\"}]",
                List.of(Map.of("colName", "region", "operator", "=", "colValue", "@region")),
                "[]");
    }

    private void assertRoleRowFilter(String responseBody, Object filters, String expectedBody) throws Exception {
        ResponseRowFilterAction action = new ResponseRowFilterAction();
        Map<String, Object> objMap = createFilterObjMap(
                responseBody,
                createClaims("teller", null),
                Constants.ROW,
                Map.of(Constants.ROLE, Map.of("teller", filters)));
        Map<String, Object> resultMap = new HashMap<>();

        action.performAction("ruleId", "actionId", objMap, resultMap, List.of());

        Assertions.assertEquals(Boolean.TRUE, resultMap.get(RuleConstants.RESULT));
        Assertions.assertEquals(expectedBody, resultMap.get("responseBody"));
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
