package com.networknt.rule;

import java.util.List;
import java.util.Map;

public interface RuleExecutor {
    Map<String, Object>  executeRule(String ruleId, Map<String, Object> input);

    Map<String, Object> executeRules(List<String> ruleIds, String logic, Map<String, Object> objMap);

    Map<String, Object> executeRules(String serviceEntry, String ruleType, Map<String, Object> objMap);

    RuleEngine getRuleEngine();

    Map<String, Object> getEndpointRules();

    void setEndpointRules(Map<String, Object> endpointRules);

    Map<String, Rule> getRules();

    void setRules(Map<String, Rule> rules);
}
