package com.networknt.rule;

import com.networknt.rule.exception.RuleEngineException;
import org.jose4j.jwt.JwtClaims;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class FineGrainedAuthAction implements IAction {
    public void performAction(String ruleId, String actionId, Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) throws RuleEngineException {
        resultMap.put(RuleConstants.RESULT, false);
        // when this action is called, we either have a client credentials token or
        // an authorization code token with roles available.
        Object allowCcObj = resultMap.get("allow-cc");
        Object allowRoleJwt = resultMap.get("allow-role-jwt");
        if(allowCcObj != null && ((Boolean)allowCcObj)) {
            resultMap.put(RuleConstants.RESULT, true);
        } else if (allowRoleJwt != null && ((Boolean)allowRoleJwt)) {
            // compare the roles with the jwt token roles
            Iterator it = actionValues.iterator();
            while(it.hasNext()) {
                RuleActionValue rav = (RuleActionValue)it.next();
                if("roles".equals(rav.getActionValueId())) {
                    String v = rav.getValue();
                    if (v != null) {
                        if(v.startsWith("$")) {
                            v = (String)objMap.get(v.substring(1));
                        }
                        // match the jwt roles with v required by the endpoint.
                        Map<String, Object> auditInfo = (Map)objMap.get("auditInfo");
                        JwtClaims jwtClaims = (JwtClaims) auditInfo.get("subject_claims");
                        String roles = (String)jwtClaims.getClaimValue("roles");
                        System.out.println("roles = " + roles + " v = " + v);
                        String[] split = roles.split("\\s+");
                        for(String s: split) {
                            if(v.indexOf(s) >= 0) {
                                resultMap.put(RuleConstants.RESULT, true);
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            // compare the roles with the transformed roles from groups in the result.
            Iterator it = actionValues.iterator();
            while(it.hasNext()) {
                RuleActionValue rav = (RuleActionValue)it.next();
                if("roles".equals(rav.getActionValueId())) {
                    String v = rav.getValue();
                    if (v != null) {
                        if(v.startsWith("$")) {
                            v = (String)objMap.get(v.substring(1));
                        }
                        // match the roles in the resultMap with v required by the endpoint.
                        for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
                            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
                            if("result".equals(entry.getKey())) continue;
                            if("allow-cc".equals(entry.getKey())) continue;
                            if("allow-role-jwt".equals(entry.getKey())) continue;
                            if(v.indexOf(entry.getKey()) >= 0 && (Boolean)entry.getValue()) {
                                resultMap.put(RuleConstants.RESULT, true);
                                break;
                            }
                        }
                    }
                }
            }

        }
    }
}
