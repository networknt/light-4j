package com.networknt.rule;

import com.networknt.rule.exception.RuleEngineException;
import com.networknt.utility.Constants;
import org.jose4j.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.scanner.Constant;

import java.util.Collection;
import java.util.Map;

/**
 * match the role claim with the roles configured in the endpoint. If there is a match, set return to true.
 */
public class RoleBasedAccessControlAction implements IAction {
    static final Logger logger = LoggerFactory.getLogger(RoleBasedAccessControlAction.class);

    @Override
    public void performAction(String ruleId, String actionId, Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) throws RuleEngineException {
        resultMap.put(RuleConstants.RESULT, false);
        // when this action is called, it is an authorization code token in the Authorization header and role
        // claim is not null. check the role in the jwt token with the required roles in the endpoint config.
        Map<String, Object> auditInfo = (Map) objMap.get(Constants.AUDIT_INFO);
        JwtClaims jwtClaims = (JwtClaims) auditInfo.get(Constants.SUBJECT_CLAIMS);
        String jwtRole = (String) jwtClaims.getClaimValue(Constants.ROLE);
        String endpointRoles = (String) objMap.get(Constants.ROLES);
        if(logger.isTraceEnabled()) logger.trace("ruleId {} actionId {} jwtRole {} endpointRoles {}", ruleId, actionId, jwtRole, endpointRoles);
        // split the role in the jwt token and compare with the required roles in the roles from the object map.
        String[] split = jwtRole.split("\\s+");
        for (String s : split) {
            if (endpointRoles.contains(s.trim())) {
                resultMap.put(RuleConstants.RESULT, true);
                break;
            }
        }
    }
}
