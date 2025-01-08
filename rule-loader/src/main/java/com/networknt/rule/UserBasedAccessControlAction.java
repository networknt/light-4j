package com.networknt.rule;

import com.networknt.rule.exception.RuleEngineException;
import com.networknt.utility.Constants;
import org.jose4j.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * match the uid claim with the users configured in the endpoint. If there is a match, set return to true.
 */

public class UserBasedAccessControlAction implements IAction{
    static final Logger logger = LoggerFactory.getLogger(UserBasedAccessControlAction.class);

    @Override
    public void performAction(String ruleId, String actionId, Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) throws RuleEngineException {
        resultMap.put(RuleConstants.RESULT, false);
        // when this action is called, it is an authorization code token in the Authorization header and uid
        // claim is not null. check the uid in the jwt token with the required users in the endpoint config.
        Map<String, Object> auditInfo = (Map) objMap.get(Constants.AUDIT_INFO);
        JwtClaims jwtClaims = (JwtClaims) auditInfo.get(Constants.SUBJECT_CLAIMS);
        String jwtUser = (String) jwtClaims.getClaimValue(Constants.UID);
        String endpointUsers = (String) objMap.get(Constants.USERS);
        if(logger.isTraceEnabled()) logger.trace("ruleId {} actionId {} jwtUser {} endpointUsers {}", ruleId, actionId, jwtUser, endpointUsers);
        String[] split = jwtUser.split("\\s+");
        for (String s : split) {
            if (endpointUsers.contains(s.trim())) {
                resultMap.put(RuleConstants.RESULT, true);
                break;
            }
        }
    }
}
