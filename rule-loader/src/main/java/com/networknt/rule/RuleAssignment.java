package com.networknt.rule;

import com.networknt.utility.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Extracts rule IDs from endpoint rule assignments. */
public final class RuleAssignment {
    private RuleAssignment() {
    }

    public static String ruleId(Object assignment) {
        Object value = assignment instanceof Map
                ? ((Map<?, ?>) assignment).get(Constants.RULE_ID) : assignment;
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Expected a rule ID or a mapping containing ruleId");
        }
        return (String) value;
    }

    public static List<String> ruleIds(List<?> assignments) {
        if (assignments == null) return null;
        List<String> ids = new ArrayList<>(assignments.size());
        for (Object assignment : assignments) {
            ids.add(ruleId(assignment));
        }
        return ids;
    }
}
