package com.networknt.rule;

import com.networknt.rule.exception.RuleEngineException;
import com.networknt.utility.Constants;
import org.jose4j.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * match the pos claim with the positions configured in the endpoint. If there is a match, set return to true.
 */

public class PositionBasedAccessControlAction implements IAction {
    static final Logger logger = LoggerFactory.getLogger(PositionBasedAccessControlAction.class);

    @Override
    public void performAction(String ruleId, String actionId, Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) throws RuleEngineException {
        resultMap.put(RuleConstants.RESULT, false);
        // when this action is called, it is an authorization code token in the Authorization header and pos
        // claim is not null. check the pos in the jwt token with the required positions in the endpoint config.
        Map<String, Object> auditInfo = (Map) objMap.get(Constants.AUDIT_INFO);
        JwtClaims jwtClaims = (JwtClaims) auditInfo.get(Constants.SUBJECT_CLAIMS);
        String jwtPosition = (String) jwtClaims.getClaimValue(Constants.POS);
        String endpointPositions = (String) objMap.get(Constants.POSITIONS);
        if(logger.isTraceEnabled()) logger.trace("ruleId {} actionId {} jwtPosition {} endpointPosition {}", ruleId, actionId, jwtPosition, endpointPositions);
        String[] split = jwtPosition.split("\\s+");
        for (String s : split) {
            if (endpointPositions.contains(s.trim())) {
                resultMap.put(RuleConstants.RESULT, true);
                break;
            }
        }
    }
}
