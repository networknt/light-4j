package com.networknt.rule;

import com.networknt.rule.exception.RuleEngineException;
import com.networknt.utility.Constants;
import com.networknt.utility.Util;
import org.jose4j.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * match the att claim with the attributes configured in the endpoint. If there is a match, set return to true.
 */

public class AttributeBasedAccessControlAction implements IAction {
    static final Logger logger = LoggerFactory.getLogger(AttributeBasedAccessControlAction.class);

    @Override
    public void performAction(String ruleId, String actionId, Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) throws RuleEngineException {
        resultMap.put(RuleConstants.RESULT, false);
        // when this action is called, it is an authorization code token in the Authorization header and att
        // claim is not null. check the att in the jwt token with the required attributes in the endpoint config.
        Map<String, Object> auditInfo = (Map) objMap.get(Constants.AUDIT_INFO);
        JwtClaims jwtClaims = (JwtClaims) auditInfo.get(Constants.SUBJECT_CLAIMS);
        String jwtAttribute = (String) jwtClaims.getClaimValue(Constants.ATT);
        List<Map<String, String>> endpointAttributes = (List<Map<String, String>>) objMap.get(Constants.ATTRIBUTES);
        if (logger.isTraceEnabled())
            logger.trace("ruleId {} actionId {} jwtAttribute {} endpointAttributes {}", ruleId, actionId, jwtAttribute, endpointAttributes);
        // parse the jwtAttribute and compare with the required attributes in the endpoint config.
        Map<String, String> jwtAttributeMap = Util.parseAttributes(jwtAttribute);
        boolean result = checkAttributesExist(endpointAttributes, jwtAttributeMap);
        if(result) {
            resultMap.put(RuleConstants.RESULT, true);
        }
    }

    public static boolean checkAttributesExist(List<Map<String, String>> endpointAttributesList, Map<String, String> jwtAttributeMap) {
        for (Map<String, String> endpointAttribute : endpointAttributesList) {
            for (Map.Entry<String, String> entry : endpointAttribute.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (!jwtAttributeMap.containsKey(key) || !jwtAttributeMap.get(key).equals(value)) {
                    return false; // Attribute not found or value mismatch
                }
            }
        }
        return true; // All attributes exist with matching values
    }
}
