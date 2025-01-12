package com.networknt.rule;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.rule.exception.RuleEngineException;
import com.networknt.utility.Constants;
import org.jose4j.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * filter the response based on the configured columns filter for the endpoint.
 */
public class ResponseColumnFilterAction implements IAction {
    static final Logger logger = LoggerFactory.getLogger(ResponseColumnFilterAction.class);

    @Override
    public void performAction(String ruleId, String actionId, Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) throws RuleEngineException {
        resultMap.put(RuleConstants.RESULT, false);
        String responseBody = (String)objMap.get("responseBody");
        if(logger.isTraceEnabled()) logger.debug("original response body = {}", responseBody);
        // get the col object from the objMap.
        Map<String, Object> colMap = (Map<String, Object>)objMap.get(Constants.COL);
        if(colMap == null) {
            if(logger.isTraceEnabled()) logger.trace("no column filter configured for the endpoint.");
            return;
        }
        if(logger.isTraceEnabled()) logger.trace("colMap = {}", colMap);
        // get the auditInfo from the objMap to get the jwt token role, group, position, attribute and user.
        Map<String, Object> auditInfo = (Map<String, Object>)objMap.get(Constants.AUDIT_INFO);
        JwtClaims jwtClaims = (JwtClaims)auditInfo.get(Constants.SUBJECT_CLAIMS);
        if(logger.isTraceEnabled()) logger.trace("jwtClaims = {}", jwtClaims);

        // convert the body from string to json map or list.
        try {
            Object body = Config.getInstance().getMapper().readValue(responseBody, Object.class);
            if(body instanceof Map) {
                Map<String, Object> bodyMap = (Map<String, Object>)body;
                Map<String, Object> filteredBodyMap = filterMapColumn(bodyMap, jwtClaims, colMap);
                responseBody = JsonMapper.toJson(filteredBodyMap);
            } else if(body instanceof List) {
                List<Object> bodyList = (List<Object>)body;
                List<Object> filteredBodyList = filterListColumn(bodyList, jwtClaims, colMap);
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

    private Map<String, Object> filterMapColumn(Map<String, Object> map, JwtClaims jwtClaims, Map<String, Object> colMap) {
        if(logger.isTraceEnabled()) logger.trace("map = {}", map);
        return map;
    }

    private List<Object> filterListColumn(List<Object> list, JwtClaims jwtClaims, Map<String, Object> colMap) {
        if(logger.isTraceEnabled()) logger.trace("list = {}", list);
        String jwtRole = jwtClaims.getClaimValueAsString(Constants.ROLE);
        Map<String, Object> roleMap = (Map<String, Object>)colMap.get(Constants.ROLE);
        if(roleMap == null) {
            if(logger.isTraceEnabled()) logger.trace("no role filter configured for the endpoint.");
        } else {
            list = mapPermission(list, roleMap, jwtRole);
        }
        String jwtGroup = jwtClaims.getClaimValueAsString(Constants.GRP);
        Map<String, Object> groupMap = (Map<String, Object>)colMap.get(Constants.GROUP);
        if(groupMap == null) {
            if(logger.isTraceEnabled()) logger.trace("no group filter configured for the endpoint.");
        } else {
            list = mapPermission(list, groupMap, jwtGroup);
        }
        String jwtPosition = jwtClaims.getClaimValueAsString(Constants.POS);
        Map<String, Object> positionMap = (Map<String, Object>)colMap.get(Constants.POSITION);
        if(positionMap == null) {
            if(logger.isTraceEnabled()) logger.trace("no position filter configured for the endpoint.");
        } else {
            list = mapPermission(list, positionMap, jwtPosition);
        }
        String jwtAttribute = jwtClaims.getClaimValueAsString(Constants.ATT);
        Map<String, Object> attributeMap = (Map<String, Object>)colMap.get(Constants.ATTRIBUTE);
        if(attributeMap == null) {
            if(logger.isTraceEnabled()) logger.trace("no attribute filter configured for the endpoint.");
        } else {
            list = mapPermission(list, attributeMap, jwtAttribute);
        }
        String jwtUser = jwtClaims.getClaimValueAsString(Constants.USER);
        Map<String, Object> userMap = (Map<String, Object>)colMap.get(Constants.USER);
        if(userMap == null) {
            if(logger.isTraceEnabled()) logger.trace("no user filter configured for the endpoint.");
        } else {
            list = mapPermission(list, userMap, jwtUser);
        }

        return list;
    }

    private static List<Object> mapPermission(List<Object> list, Map<String, Object> permissionMap, String jwtPermission) {
        if(logger.isTraceEnabled()) logger.trace("permissionMap = {} jwtPermission = {}", permissionMap, jwtPermission);
        for (Map.Entry<String, Object> entry : permissionMap.entrySet()) {
            String key = entry.getKey();
            String value = (String)entry.getValue();
            if(jwtPermission.contains(key)) {
                if(logger.isTraceEnabled()) logger.trace("permission matched for key = {} value = {}", key, value);
                // filter the list based on the value
                if(value.startsWith("!")) {
                    // remove the "!"
                    value = value.substring(1);
                    List<String> removeList = convertStringToList(value);
                    if(removeList != null) {
                        for (Object o : list) {
                            Map<String, Object> map = (Map<String, Object>)o;
                            for (String s : removeList) {
                                map.remove(s);
                            }
                        }
                    } else {
                        logger.error("Invalid value for the column filter: {}", value);
                    }
                } else {
                    List<String> keepList = convertStringToList(value);
                    if(keepList != null) {
                        for (Object o : list) {
                            Map<String, Object> map = (Map<String, Object>)o;
                            map.keySet().retainAll(keepList);
                        }
                    } else {
                        logger.error("Invalid value for the column filter: {}", value);
                    }
                }
            }
        }
        return list;
    }

    private static List<String> convertStringToList(String inputString){
        try {
            return Config.getInstance().getMapper().readValue(inputString, new TypeReference<List<String>>(){});
        } catch (IOException e) {
            logger.error("IOException:", e);
            return null;
        }
    }
}
