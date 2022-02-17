package com.networknt.rule;

import org.jose4j.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * This is the action that is being called after the {service}-group-role rules. It picks up the roles that is true in the result
 * and concat them together as a roles value to put it into the objMap for the next rule like portal-role-access etc.
 *
 */
public class GroupRoleTransformAction implements IAction {
    private static final Logger logger = LoggerFactory.getLogger(GroupRoleTransformAction.class);
    public void performAction(Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        // need to make sure that the result is true.
        boolean result = (Boolean)resultMap.get(RuleConstants.RESULT);
        if(result) {
            String roles = null;
            for (Map.Entry<String,Object> entry : resultMap.entrySet()) {
                if(logger.isDebugEnabled()) logger.debug("key = " + entry.getKey() + " value = " + entry.getValue());
                if((Boolean)entry.getValue() && !entry.getKey().equals(RuleConstants.RESULT)) {
                    if(roles == null) {
                        roles = entry.getKey();
                    } else {
                        roles = roles + " " + entry.getKey();
                    }
                }
            }
            // put this into the input map for the next rule to work with roles instead of groups.
            Map auditInfo = (Map)objMap.get("auditInfo");
            JwtClaims claims = (JwtClaims)auditInfo.get("subject_claims");
            claims.setClaim("roles", roles);
        }
    }
}
