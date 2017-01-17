/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.networknt.registry.support.command;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcCommandUtil {
    private static final Logger logger = LoggerFactory.getLogger(RpcCommand.class);
    /**
     * String to Command
     *
     * @param commandString string representation of rpc command
     * @return RpcCommand rpc command
     */
    public static RpcCommand stringToCommand(String commandString) {
        try {
            return Config.getInstance().getMapper().readValue(commandString, RpcCommand.class);
        } catch (Exception e) {
            logger.error("Invalid JSON format " + commandString);
            return null;
        }
    }

    /**
     * Command to String
     *
     * @param command rpc command object
     * @return a string presentation of rpc command
     * @throws JsonProcessingException exception while processing JSON
     */
    public static String commandToString(RpcCommand command) throws JsonProcessingException {
        return Config.getInstance().getMapper().writeValueAsString(command);
    }

    private static PatternEvaluator evaluator = new PatternEvaluator();

    public static boolean match(String expression, String path) {
        if (expression == null || expression.length() == 0) {
            return false;
        }
        return evaluator.match(expression, path);
    }


    private static class PatternEvaluator {

        Pattern pattern = Pattern.compile("[a-zA-Z0-9_$.*]+");
        Set<Character> all = ImmutableSet.of('(', ')', '0', '1', '!', '&', '|');
        Map<Character, ImmutableSet<Character>> following = ImmutableMap.<Character, ImmutableSet<Character>>builder()
                .put('(', ImmutableSet.of('0', '1', '!')).put(')', ImmutableSet.of('|', '&', ')')).put('0', ImmutableSet.of('|', '&', ')'))
                .put('1', ImmutableSet.of('|', '&', ')')).put('!', ImmutableSet.of('(', '0', '1', '!'))
                .put('&', ImmutableSet.of('(', '0', '1', '!')).put('|', ImmutableSet.of('(', '0', '1', '!')).build();

        boolean match(String expression, String path) {

            // match each item and replace with 0 or 1
            Matcher matcher = pattern.matcher(expression.replaceAll("\\s+", ""));
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String s = matcher.group();
                int idx = s.indexOf('*');
                if (idx != -1) {
                    matcher.appendReplacement(buffer, path.startsWith(s.substring(0, idx)) ? "1" : "0");
                } else {
                    matcher.appendReplacement(buffer, s.equals(path) ? "1" : "0");
                }
            }
            matcher.appendTail(buffer);
            String result1 = buffer.toString();

            LinkedList<LinkedList<Character>> outer = new LinkedList<LinkedList<Character>>();
            LinkedList<Character> inner = new LinkedList<Character>();
            inner.push('#');
            outer.push(inner);

            int i = 0;
            int len = result1.length();
            while (outer.size() > 0 && i < len) {
                LinkedList<Character> sub = outer.peekLast();
                while (sub.size() > 0 && i < len) {
                    char curr = result1.charAt(i++);
                    support(curr);
                    char prev = sub.peekFirst();
                    if (prev != '#') {
                        supportFollowing(prev, curr);
                    }

                    switch (curr) {
                        case '(':
                            sub = new LinkedList<Character>();
                            sub.push('#');
                            outer.push(sub);
                            break;
                        case ')':
                            outer.removeFirst();
                            outer.peekFirst().push(evalWithinParentheses(sub));
                            sub = outer.peekFirst();
                            break;
                        default:
                            sub.push(curr);
                    }
                }
            }
            if (outer.size() != 1) {
                throw new IllegalArgumentException("Syntax error!");
            }
            char result = evalWithinParentheses(outer.peekLast());
            return result == '1';
        }

        /**
         * eval within parentheses
         *
         * @param list
         * @return char
         */
        char evalWithinParentheses(LinkedList<Character> list) {
            char operand = list.pop();
            if (operand != '0' && operand != '1') {
                syntaxError();
            }

            // 处理!
            while (!list.isEmpty()) {
                char curr = list.pop();
                if (curr == '!') {
                    operand = operand == '0' ? '1' : '0';
                } else if (curr == '#') {
                    break;
                } else {
                    if (operand == '0' || operand == '1') {
                        list.addLast(operand);
                        list.addLast(curr);
                        operand = '\0';
                    } else {
                        operand = curr;
                    }
                }
            }
            list.addLast(operand);

            // handle &
            list.addLast('#');
            operand = list.pop();
            while (!list.isEmpty()) {
                char curr = list.pop();
                if (curr == '&') {
                    char c = list.pop();
                    operand = (operand == '1' && c == '1') ? '1' : '0';
                } else if (curr == '#') {
                    break;
                } else {
                    if (operand == '0' || operand == '1') {
                        list.addLast(operand);
                        list.addLast(curr);
                        operand = '\0';
                    } else {
                        operand = curr;
                    }
                }
            }
            list.addLast(operand);

            // handle |
            operand = '0';
            while (!list.isEmpty() && (operand = list.pop()) != '1');
            return operand;
        }

        void syntaxError() {
            throw new IllegalArgumentException("Syntax error! only support ()!&| in priority.");
        }

        void syntaxError(String s) {
            throw new IllegalArgumentException("Syntax error: " + s);
        }

        void support(char c) {
            if (!all.contains(c)) {
                syntaxError("Unsupported Character " + c);
            }
        }

        void supportFollowing(char prev, char c) {
            if (!following.get(prev).contains(c)) {
                syntaxError("prev=" + prev + ", c=" + c);
            }
        }
    }

}
