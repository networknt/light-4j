package com.networknt.rule;

import com.networknt.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MultiThreadRuleExecutor implements RuleExecutor {
    private static final Logger logger = LoggerFactory.getLogger(MultiThreadRuleExecutor.class);
    private static MultiThreadRuleExecutor instance;
    private Map<String, Object> endpointRules;
    private Map<String, Rule> rules;
    private RuleEngine ruleEngine;

    public MultiThreadRuleExecutor() {
        RuleConfig ruleConfig = RuleConfig.load();
        // Load rule bodies from RuleConfig
        Map<String, Object> ruleBodies = ruleConfig.getRuleBodies();
        if (ruleBodies != null && !ruleBodies.isEmpty()) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            String ruleString = new Yaml(options).dump(ruleBodies);
            rules = RuleMapper.string2RuleMap(ruleString);
            if (logger.isInfoEnabled()) logger.info("Load YAML rules from RuleConfig with size = {}", rules.size());
        }

        // Load endpoint rules from RuleConfig (now a Map directly)
        endpointRules = ruleConfig.getEndpointRules();
        if (endpointRules == null) {
            endpointRules = new HashMap<>();
        }

        if (rules != null) {
            // create the rule engine with the rule map.
            ruleEngine = new RuleEngine(rules, null);
            // iterate all action classes to initialize them to ensure that the jar file are deployed and configuration is registered.
            loadPluginClass();
        }
    }

    protected MultiThreadRuleExecutor(Map<String, Rule> rules, RuleEngine ruleEngine) {
        this.rules = rules;
        this.ruleEngine = ruleEngine;
    }

    @Override
    public Map<String, Rule> getRules() {
        return rules;
    }

    @Override
    public void setRules(Map<String, Rule> rules) {
        this.rules = rules;
    }

    @Override
    public Map<String, Object> getEndpointRules() {
        return endpointRules;
    }

    @Override
    public void setEndpointRules(Map<String, Object> endpointRules) {
        this.endpointRules = endpointRules;
    }

    @Override
    public RuleEngine getRuleEngine() {
        return ruleEngine;
    }

    private void loadPluginClass() {
        // iterate the rules map to find the action classes.
        if(rules != null) {
            for(Rule rule: rules.values()) {
                if(rule.getActions() != null) {
                    for (RuleAction action : rule.getActions()) {
                        String actionClass = action.getActionClassName();
                        loadActionClass(actionClass);
                    }
                }
            }
        }
    }

    private void loadActionClass(String actionClass) {
        if(logger.isDebugEnabled()) logger.debug("load action class {}", actionClass);
        try {
            IAction ia = (IAction)Class.forName(actionClass).getDeclaredConstructor().newInstance();
            // this happens during the server startup, so the cache must be empty. No need to check.
            ruleEngine.actionClassCache.put(actionClass, ia);
        } catch (Exception e) {
            logger.error("Exception:", e);
            throw new RuntimeException("Could not find rule action class " + actionClass, e);
        }
    }

    @Override
    public Map<String, Object> executeRule(String ruleId, Map<String, Object> input) {
        try {
            return ruleEngine.executeRule(ruleId, input);
        } catch (Exception e) {
            logger.error("Exception ruleId {} input {}", ruleId, input, e);
            Map<String, Object> result = new HashMap<>();
            result.put(RuleConstants.RESULT, false);
            result.put(Constants.ERROR_MESSAGE, e.getMessage());
            return result;
        }
    }

    @Override
    public Map<String, Object> executeRules(List<String> ruleIds, String logic, Map<String, Object> objMap) {
        if (ruleIds == null || ruleIds.isEmpty()) {
            return null;
        }

        if (logic != null && logic.equalsIgnoreCase("all")) {
            // Sequential execution for "all" logic as rules might depend on each other's outcomes
            // or one failure should stop the execution.
            Map<String, Object> lastResult = null;
            for (String ruleId : ruleIds) {
                try {
                    lastResult = ruleEngine.executeRule(ruleId, objMap);
                } catch (Exception e) {
                    logger.error("Exception ruleId {} objMap {}", ruleId, objMap, e);
                    lastResult = new HashMap<>();
                    lastResult.put(RuleConstants.RESULT, false);
                    lastResult.put(Constants.ERROR_MESSAGE, e.getMessage());
                }
                boolean res = (Boolean) lastResult.get(RuleConstants.RESULT);
                if (!res) {
                    return lastResult;
                }
            }
            return lastResult;
        } else if (logic != null && logic.equalsIgnoreCase("any")) {
            // Sequential execution for "any" logic
            for (String ruleId : ruleIds) {
                try {
                    Map<String, Object> result = ruleEngine.executeRule(ruleId, objMap);
                    boolean res = (Boolean) result.get(RuleConstants.RESULT);
                    if (res) {
                        return result;
                    }
                } catch (Exception e) {
                    logger.error("Exception ruleId {} objMap {}", ruleId, objMap, e);
                }
            }
            return null;
        } else {
            // Parallel execution for multiple rules if logic is not specified or different
            List<CompletableFuture<Map<String, Object>>> futures = ruleIds.stream()
                    .map(ruleId -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return ruleEngine.executeRule(ruleId, objMap);
                        } catch (Exception e) {
                            logger.error("Exception ruleId {} objMap {}", ruleId, objMap, e);
                            Map<String, Object> result = new HashMap<>();
                            result.put(RuleConstants.RESULT, false);
                            result.put(Constants.ERROR_MESSAGE, e.getMessage());
                            return result;
                        }
                    }))
                    .collect(Collectors.toList());

            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            return allOf.thenApply(v -> {
                Map<String, Object> mergedResult = new HashMap<>();
                for (CompletableFuture<Map<String, Object>> future : futures) {
                    Map<String, Object> result = future.join();
                    if (result != null) {
                        mergedResult.putAll(result);
                    }
                }
                return mergedResult;
            }).join();
        }
    }

    @Override
    public Map<String, Object> executeRules(String serviceEntry, String ruleType, Map<String, Object> objMap) {
        if (endpointRules == null) {
            logger.error("endpointRules is null");
            return null;
        }

        Map<String, Object> rulesConfig = (Map<String, Object>) endpointRules.get(serviceEntry);
        if (rulesConfig == null) {
            if (logger.isDebugEnabled()) logger.debug("No rules found for serviceEntry: {}", serviceEntry);
            return null;
        }

        List<String> ruleIds = (List<String>) rulesConfig.get(ruleType);
        if (ruleIds == null || ruleIds.isEmpty()) {
            if (logger.isDebugEnabled()) logger.debug("No rules found for type: {} in serviceEntry: {}", ruleType, serviceEntry);
            return null;
        }

        // Add permissions to objMap if they exist
        Map<String, Object> permissionMap = (Map<String, Object>) rulesConfig.get("permission");
        if (permissionMap != null) {
            objMap.putAll(permissionMap);
            // Some handlers use COL and ROW explicitly
            if (permissionMap.containsKey(Constants.COL)) objMap.put(Constants.COL, permissionMap.get(Constants.COL));
            if (permissionMap.containsKey(Constants.ROW)) objMap.put(Constants.ROW, permissionMap.get(Constants.ROW));
        }

        // Logic might be provided from config or default based on ruleType
        String logic = "parallel"; // Default for transformation/filtering
        if (ruleType.equals("req-acc")) {
            // For access control, we probably want "any" or "all" based on config.
            // But we don't have the config here. We should probably pass it or rely on executeRules(List, String, Map).
            // RequestTransformerInterceptor uses sequential execution implicitely for req-tra.
        }

        return executeRules(ruleIds, logic, objMap);
    }
}
