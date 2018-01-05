package com.networknt.utility;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

    /**
     * Check if the string is null or empty
     *
     * @param value the value that is checked
     * @return true if the given value is either null or the empty string
     *
     */
    public static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public static String expandEnvVars(String text) {
        Map<String, String> envMap = System.getenv();
        String pattern = "\\$\\{([A-Za-z0-9-_]+)\\}";
        Pattern expr = Pattern.compile(pattern);
        Matcher matcher = expr.matcher(text);
        while (matcher.find()) {
            String envValue = envMap.get(matcher.group(1).toUpperCase());
            if (envValue == null) {
                envValue = "";
            } else {
                envValue = envValue.replace("\\", "\\\\");
            }
            Pattern subexpr = Pattern.compile(Pattern.quote(matcher.group(0)));
            text = subexpr.matcher(text).replaceAll(envValue);
        }
        return text;
    }
}
