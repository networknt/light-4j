package com.networknt.rule;

import com.networknt.rule.exception.RuleEngineException;
import com.networknt.utility.Constants;
import org.jose4j.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * match the grp claim with the group configured in the endpoint. If there is a match, set return to true.
 */

public class GroupBasedAccessControlAction implements IAction {
    static final Logger logger = LoggerFactory.getLogger(GroupBasedAccessControlAction.class);

    @Override
    public void performAction(String ruleId, String actionId, Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) throws RuleEngineException {
        resultMap.put(RuleConstants.RESULT, false);
        // when this action is called, it is an authorization code token in the Authorization header and grp
        // claim is not null. check the grp in the jwt token with the required groups in the endpoint config.
        Map<String, Object> auditInfo = (Map) objMap.get(Constants.AUDIT_INFO);
        JwtClaims jwtClaims = (JwtClaims) auditInfo.get(Constants.SUBJECT_CLAIMS);
        String jwtGroup = (String) jwtClaims.getClaimValue(Constants.GRP);
        String endpointGroups = (String) objMap.get(Constants.GROUPS);
        if (logger.isTraceEnabled())
            logger.trace("ruleId {} actionId {} jwtGroup {} endpointGroup {}", ruleId, actionId, jwtGroup, endpointGroups);
        // split the grp in the jwt token and compare with the required group in the groups from the object map.
        String[] split = jwtGroup.split("\\s+");
        for (String s : split) {
            if (endpointGroups.contains(s.trim())) {
                resultMap.put(RuleConstants.RESULT, true);
                break;
            }
        }

    }

}
