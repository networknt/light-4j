package com.networknt.rule;

import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.rule.exception.RuleEngineException;
import com.networknt.utility.Constants;
import org.jose4j.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * row filter the response based on the configured filter for the endpoint.
 */
public class ResponseRowFilterAction implements IAction {
    static final Logger logger = LoggerFactory.getLogger(ResponseRowFilterAction.class);

    @Override
    public void performAction(String ruleId, String actionId, Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) throws RuleEngineException {
        resultMap.put(RuleConstants.RESULT, false);
        String responseBody = (String)objMap.get("responseBody");
        if(logger.isTraceEnabled()) logger.debug("original response body = {}", responseBody);
        // get the col object from the objMap.
        Map<String, Object> rowMap = (Map<String, Object>)objMap.get(Constants.ROW);
        if(rowMap == null) {
            if(logger.isTraceEnabled()) logger.trace("no row filter configured for the endpoint.");
            return;
        }
        if(logger.isTraceEnabled()) logger.trace("rowMap = {}", rowMap);
        // get the auditInfo from the objMap to get the jwt token role, group, position, attribute and user.
        Map<String, Object> auditInfo = (Map<String, Object>)objMap.get(Constants.AUDIT_INFO);
        JwtClaims jwtClaims = (JwtClaims)auditInfo.get(Constants.SUBJECT_CLAIMS);
        if(logger.isTraceEnabled()) logger.trace("jwtClaims = {}", jwtClaims);

        // convert the body from string to json map or list.
        try {
            Object body = Config.getInstance().getMapper().readValue(responseBody, Object.class);
            if(body instanceof Map) {
                Map<String, Object> bodyMap = (Map<String, Object>)body;
                Map<String, Object> filteredBodyMap = filterMapRow(bodyMap, jwtClaims, rowMap);
                responseBody = JsonMapper.toJson(filteredBodyMap);
            } else if(body instanceof List) {
                List<Object> bodyList = (List<Object>)body;
                List<Object> filteredBodyList = filterListRow(bodyList, jwtClaims, rowMap);
                responseBody = JsonMapper.toJson(filteredBodyList);
            } else {
                // if the body is not a map or list, then it is a string, and we cannot encode it.
                if(logger.isTraceEnabled()) logger.trace("response body is not a map or list, skip filtering.");
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if(logger.isTraceEnabled()) logger.trace("filtered response body = {}", responseBody);
        resultMap.put(RuleConstants.RESULT, true);
        resultMap.put("responseBody", responseBody);
    }

    private Map<String, Object> filterMapRow(Map<String, Object> map, JwtClaims jwtClaims, Map<String, Object> rowMap) {
        if(logger.isTraceEnabled()) logger.trace("map = {}", map);

        return map;
    }

    private List<Object> filterListRow(List<Object> list, JwtClaims jwtClaims, Map<String, Object> rowMap) {
        if(logger.isTraceEnabled()) logger.trace("list = {}", list);
        String jwtRole = jwtClaims.getClaimValueAsString(Constants.ROLE);
        Map<String, Object> roleMap = (Map<String, Object>)rowMap.get(Constants.ROLE);
        if(roleMap == null) {
            if(logger.isTraceEnabled()) logger.trace("no role filter configured for the endpoint.");
        } else {
            list = mapPermission(list, roleMap, jwtRole, jwtClaims);
        }
        String jwtGroup = jwtClaims.getClaimValueAsString(Constants.GRP);
        Map<String, Object> groupMap = (Map<String, Object>)rowMap.get(Constants.GROUP);
        if(groupMap == null) {
            if(logger.isTraceEnabled()) logger.trace("no group filter configured for the endpoint.");
        } else {
            list = mapPermission(list, groupMap, jwtGroup, jwtClaims);
        }
        String jwtPosition = jwtClaims.getClaimValueAsString(Constants.POS);
        Map<String, Object> positionMap = (Map<String, Object>)rowMap.get(Constants.POSITION);
        if(positionMap == null) {
            if(logger.isTraceEnabled()) logger.trace("no position filter configured for the endpoint.");
        } else {
            list = mapPermission(list, positionMap, jwtPosition, jwtClaims);
        }
        String jwtAttribute = jwtClaims.getClaimValueAsString(Constants.ATT);
        Map<String, Object> attributeMap = (Map<String, Object>)rowMap.get(Constants.ATTRIBUTE);
        if(attributeMap == null) {
            if(logger.isTraceEnabled()) logger.trace("no attribute filter configured for the endpoint.");
        } else {
            list = mapPermission(list, attributeMap, jwtAttribute, jwtClaims);
        }
        String jwtUser = jwtClaims.getClaimValueAsString(Constants.USER);
        Map<String, Object> userMap = (Map<String, Object>)rowMap.get(Constants.USER);
        if(userMap == null) {
            if(logger.isTraceEnabled()) logger.trace("no user filter configured for the endpoint.");
        } else {
            list = mapPermission(list, userMap, jwtUser, jwtClaims);
        }
        return list;
    }

    private static List<Object> mapPermission(List<Object> list, Map<String, Object> permissionMap, String jwtPermission, JwtClaims jwtClaims) {
        if (logger.isTraceEnabled()) logger.trace("permissionMap = {} jwtPermission = {}", permissionMap, jwtPermission);
        for (Map.Entry<String, Object> entry : permissionMap.entrySet()) {
            String key = entry.getKey();  // this is a role, group, position, attribute, user
            List<Map<String, Object>> value = (List<Map<String, Object>>) entry.getValue(); // this is the list of colName, operator and colValue map.
            if (jwtPermission.contains(key)) {
                if (logger.isTraceEnabled()) logger.trace("permission matched for key = {} value = {}", key, value);

                Iterator<Object> iterator = list.iterator();
                while (iterator.hasNext()) {
                    Map<String, Object> map = (Map<String, Object>) iterator.next();
                    boolean shouldRemove = false;
                    for (Map<String, Object> filterMap : value) {
                        String colName = (String) filterMap.get("colName");
                        String operator = (String) filterMap.get("operator");
                        String colValue = (String) filterMap.get("colValue");
                        if(colValue.startsWith("@")) colValue = jwtClaims.getClaimValueAsString(colValue.substring(1));
                        if (map.containsKey(colName)) {
                            String itemValue = (String) map.get(colName);
                            if (!matchFilterWithString(itemValue, operator, colValue)) {
                                shouldRemove = true;
                                break;
                            }
                        }
                    }
                    if (shouldRemove) {
                        iterator.remove(); // Correctly remove using the iterator
                    }
                }
            }
        }
        return list;
    }

    private static boolean matchFilterWithString(Object itemValue, String filterOp, String filterValue){
        String itemString = String.valueOf(itemValue);
        switch (filterOp){
            case "=":
                return itemString.equals(filterValue);
            case "!=":
                return !itemString.equals(filterValue);
            case "in":
                if(filterValue.startsWith("[") && filterValue.endsWith("]")){
                    String filterValueList = filterValue.substring(1, filterValue.length()-1);
                    List<String> values = Arrays.stream(filterValueList.split(","))
                            .map(String::trim)
                            .collect(Collectors.toList());
                    return values.contains(itemString);
                }
                return false;
            case "not in":
                if(filterValue.startsWith("[") && filterValue.endsWith("]")){
                    String filterValueList = filterValue.substring(1, filterValue.length()-1);
                    List<String> values = Arrays.stream(filterValueList.split(","))
                            .map(String::trim)
                            .collect(Collectors.toList());
                    return !values.contains(itemString);
                }
                return false;
            default:
                return true;
        }
    }

    private boolean matchFilterWithNumber(Number itemValue, String filterOp, String filterValue){
        double itemDouble = itemValue.doubleValue();
        double filterDouble = Double.parseDouble(filterValue);
        switch (filterOp){
            case "=":
                return itemDouble == filterDouble;
            case "!=":
                return itemDouble != filterDouble;
            case "<":
                return itemDouble < filterDouble;
            case ">":
                return itemDouble > filterDouble;
            case "<=":
                return itemDouble <= filterDouble;
            case ">=":
                return itemDouble >= filterDouble;
            case "range":
                if(filterValue.startsWith("[") && filterValue.endsWith("]")){
                    String filterValueList = filterValue.substring(1, filterValue.length()-1);
                    List<String> values = Arrays.stream(filterValueList.split(","))
                            .map(String::trim)
                            .collect(Collectors.toList());
                    if(values.size() == 2){
                        double minValue = Double.parseDouble(values.get(0));
                        double maxValue = Double.parseDouble(values.get(1));
                        return itemDouble >= minValue && itemDouble <= maxValue;
                    }
                    return false;
                }
                return false;
            default:
                return true;
        }
    }

}
