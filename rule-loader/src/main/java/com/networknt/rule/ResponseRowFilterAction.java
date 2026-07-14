package com.networknt.rule;

import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.rule.exception.RuleEngineException;
import com.networknt.utility.Constants;
import org.jose4j.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * row filter the response based on the configured filter for the endpoint.
 */
public class ResponseRowFilterAction implements IAction {
    static final Logger logger = LoggerFactory.getLogger(ResponseRowFilterAction.class);

    @Override
    public void performAction(String ruleId, String actionId, Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) throws RuleEngineException {
        resultMap.put(RuleConstants.RESULT, false);
        String responseBody = (String)objMap.get("responseBody");
        if(logger.isTraceEnabled()) logger.debug("original response body = {}", responseBody);
        // get the col object from the objMap.
        Map<String, Object> rowMap = (Map<String, Object>)objMap.get(Constants.ROW);
        if(rowMap == null) {
            if(logger.isTraceEnabled()) logger.trace("no row filter configured for the endpoint.");
            return;
        }
        if(logger.isTraceEnabled()) logger.trace("rowMap = {}", rowMap);
        // get the auditInfo from the objMap to get the jwt token role, group, position, attribute and user.
        Map<String, Object> auditInfo = (Map<String, Object>)objMap.get(Constants.AUDIT_INFO);
        JwtClaims jwtClaims = (JwtClaims)auditInfo.get(Constants.SUBJECT_CLAIMS);
        if(logger.isTraceEnabled()) logger.trace("jwtClaims = {}", jwtClaims);

        // convert the body from string to json map or list.
        try {
            Object body = Config.getInstance().getMapper().readValue(responseBody, Object.class);
            if(body instanceof Map) {
                Map<String, Object> bodyMap = (Map<String, Object>)body;
                Map<String, Object> filteredBodyMap = filterMapRow(bodyMap, jwtClaims, rowMap);
                responseBody = JsonMapper.toJson(filteredBodyMap);
            } else if(body instanceof List) {
                List<Object> bodyList = (List<Object>)body;
                List<Object> filteredBodyList = filterListRow(bodyList, jwtClaims, rowMap);
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

    private Map<String, Object> filterMapRow(Map<String, Object> map, JwtClaims jwtClaims, Map<String, Object> rowMap) {
        if(logger.isTraceEnabled()) logger.trace("map = {}", map);

        return map;
    }

    private List<Object> filterListRow(List<Object> list, JwtClaims jwtClaims, Map<String, Object> rowMap) {
        if(logger.isTraceEnabled()) logger.trace("list = {}", list);
        String jwtRole = jwtClaims.getClaimValueAsString(Constants.ROLE);
        Map<String, Object> roleMap = (Map<String, Object>)rowMap.get(Constants.ROLE);
        if(roleMap == null) {
            if(logger.isTraceEnabled()) logger.trace("no role filter configured for the endpoint.");
        } else {
            list = mapPermission(list, roleMap, jwtRole, jwtClaims);
        }
        String jwtGroup = jwtClaims.getClaimValueAsString(Constants.GRP);
        Map<String, Object> groupMap = (Map<String, Object>)rowMap.get(Constants.GROUP);
        if(groupMap == null) {
            if(logger.isTraceEnabled()) logger.trace("no group filter configured for the endpoint.");
        } else {
            list = mapPermission(list, groupMap, jwtGroup, jwtClaims);
        }
        String jwtPosition = jwtClaims.getClaimValueAsString(Constants.POS);
        Map<String, Object> positionMap = (Map<String, Object>)rowMap.get(Constants.POSITION);
        if(positionMap == null) {
            if(logger.isTraceEnabled()) logger.trace("no position filter configured for the endpoint.");
        } else {
            list = mapPermission(list, positionMap, jwtPosition, jwtClaims);
        }
        String jwtAttribute = jwtClaims.getClaimValueAsString(Constants.ATT);
        Map<String, Object> attributeMap = (Map<String, Object>)rowMap.get(Constants.ATTRIBUTE);
        if(attributeMap == null) {
            if(logger.isTraceEnabled()) logger.trace("no attribute filter configured for the endpoint.");
        } else {
            list = mapPermission(list, attributeMap, jwtAttribute, jwtClaims);
        }
        String jwtUser = jwtClaims.getClaimValueAsString(Constants.UID);
        Map<String, Object> userMap = (Map<String, Object>)rowMap.get(Constants.USER);
        if(userMap == null) {
            if(logger.isTraceEnabled()) logger.trace("no user filter configured for the endpoint.");
        } else {
            list = mapPermission(list, userMap, jwtUser, jwtClaims);
        }
        return list;
    }

    private static List<Object> mapPermission(List<Object> list, Map<String, Object> permissionMap, String jwtPermission, JwtClaims jwtClaims) {
        if (logger.isTraceEnabled()) logger.trace("permissionMap = {} jwtPermission = {}", permissionMap, jwtPermission);
        List<List<RowFilter>> matchedFilterGroups = new ArrayList<>();
        for (Map.Entry<String, Object> entry : permissionMap.entrySet()) {
            String key = entry.getKey();  // this is a role, group, position, attribute, user
            if (PermissionMatchUtils.hasPermission(jwtPermission, key)) {
                Optional<List<RowFilter>> parsedFilters = parseFilters(entry.getValue(), jwtClaims);
                if (logger.isTraceEnabled()) logger.trace("permission matched for key = {} value = {}", key, entry.getValue());
                if (parsedFilters.isEmpty()) {
                    logger.warn("Matched response row filter for permission {} is empty or invalid; all rows will be denied", key);
                }
                matchedFilterGroups.add(parsedFilters.orElseGet(Collections::emptyList));
            }
        }
        if (!matchedFilterGroups.isEmpty()) {
            list.removeIf(item -> !matchedFilterGroups.stream().allMatch(filters -> rowMatches(item, filters)));
        }
        return list;
    }

    private static Optional<List<RowFilter>> parseFilters(Object configuredValue, JwtClaims jwtClaims) {
        if (!(configuredValue instanceof List<?> configuredFilters) || configuredFilters.isEmpty()) {
            return Optional.empty();
        }
        List<RowFilter> filters = new ArrayList<>(configuredFilters.size());
        for (Object configuredFilter : configuredFilters) {
            if (!(configuredFilter instanceof Map<?, ?> filterMap)) {
                return Optional.empty();
            }
            Object colNameValue = filterMap.get("colName");
            Object operatorValue = filterMap.get("operator");
            Object colValueValue = filterMap.get("colValue");
            if (!(colNameValue instanceof String colName) || colName.isBlank()
                    || (operatorValue != null && !(operatorValue instanceof String))
                    || !isScalar(colValueValue)) {
                return Optional.empty();
            }
            String operator = operatorValue == null ? "=" : (String) operatorValue;
            if (!isSupportedOperator(operator)) {
                return Optional.empty();
            }
            String colValue = String.valueOf(colValueValue);
            if (colValue.startsWith("@")) {
                colValue = jwtClaims.getClaimValueAsString(colValue.substring(1));
                if (colValue == null) {
                    return Optional.empty();
                }
            }
            List<String> listValues = switch (operator) {
                case "in", "not in" -> listTokens(colValue);
                default -> Collections.emptyList();
            };
            filters.add(new RowFilter(colName, operator, colValue, listValues));
        }
        return Optional.of(filters);
    }

    private static boolean rowMatches(Object item, List<RowFilter> filters) {
        if (!(item instanceof Map<?, ?> map) || filters.isEmpty()) {
            return false;
        }
        return filters.stream().allMatch(filter -> map.containsKey(filter.colName())
                && matchFilter(map.get(filter.colName()), filter));
    }

    private static boolean isScalar(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean;
    }

    private static boolean isSupportedOperator(String operator) {
        return switch (operator) {
            case "=", "!=", "<", ">", "<=", ">=", "in", "not in", "range" -> true;
            default -> false;
        };
    }

    private static boolean matchFilter(Object itemValue, RowFilter filter) {
        String filterOp = filter.operator();
        String filterValue = filter.colValue();
        if (itemValue == null
                || itemValue instanceof Map<?, ?> || itemValue instanceof Collection<?>) {
            return false;
        }
        if (itemValue instanceof Number number && isNumericOperator(filterOp)) {
            return matchFilterWithNumber(number, filterOp, filterValue);
        }
        return matchFilterWithString(itemValue, filterOp, filterValue, filter.listValues());
    }

    private static boolean isNumericOperator(String filterOp) {
        return switch (filterOp) {
            case "=", "!=", "<", ">", "<=", ">=", "range" -> true;
            default -> false;
        };
    }

    private static boolean matchFilterWithString(Object itemValue, String filterOp, String filterValue, List<String> listValues){
        if (itemValue == null || filterOp == null || filterValue == null) {
            return false;
        }
        String itemString = String.valueOf(itemValue);
        switch (filterOp){
            case "=":
                return itemString.equals(filterValue);
            case "!=":
                return !itemString.equals(filterValue);
            case "in":
                return listValues.contains(itemString);
            case "not in":
                return !listValues.contains(itemString);
            default:
                return false;
        }
    }

    private static boolean matchFilterWithNumber(Number itemValue, String filterOp, String filterValue){
        if (itemValue == null || filterOp == null || filterValue == null) {
            return false;
        }
        double itemDouble = itemValue.doubleValue();
        if ("range".equals(filterOp)) {
            if (!filterValue.startsWith("[") || !filterValue.endsWith("]")) {
                return false;
            }
            String filterValueList = filterValue.substring(1, filterValue.length() - 1);
            List<String> values = Arrays.stream(filterValueList.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
            if (values.size() != 2) {
                return false;
            }
            try {
                double minValue = Double.parseDouble(values.get(0));
                double maxValue = Double.parseDouble(values.get(1));
                return itemDouble >= minValue && itemDouble <= maxValue;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        final double filterDouble;
        try {
            filterDouble = Double.parseDouble(filterValue);
        } catch (NumberFormatException e) {
            return false;
        }
        switch (filterOp){
            case "=":
                return itemDouble == filterDouble;
            case "!=":
                return itemDouble != filterDouble;
            case "<":
                return itemDouble < filterDouble;
            case ">":
                return itemDouble > filterDouble;
            case "<=":
                return itemDouble <= filterDouble;
            case ">=":
                return itemDouble >= filterDouble;
            default:
                return false;
        }
    }

    private static List<String> listTokens(String value) {
        String normalized = value.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            try {
                Object parsed = Config.getInstance().getMapper().readValue(normalized, Object.class);
                if (parsed instanceof List<?> values) {
                    return values.stream()
                            .filter(ResponseRowFilterAction::isScalar)
                            .map(String::valueOf)
                            .map(String::trim)
                            .filter(token -> !token.isEmpty())
                            .collect(Collectors.toList());
                }
            } catch (Exception ignored) {
                // Preserve the legacy bracketed form, for example [New York, CA].
            }
            normalized = normalized.substring(1, normalized.length() - 1);
            return Arrays.stream(normalized.split(","))
                    .map(String::trim)
                    .map(ResponseRowFilterAction::stripMatchingQuotes)
                    .filter(token -> !token.isEmpty())
                    .collect(Collectors.toList());
        }
        if (normalized.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(normalized.split("[,\\s]+"))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toList());
    }

    private static String stripMatchingQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private record RowFilter(String colName, String operator, String colValue, List<String> listValues) {}

}
