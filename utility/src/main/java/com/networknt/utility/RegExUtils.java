/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.networknt.utility;

import java.util.regex.Pattern;

/**
 * <p>Helpers to process Strings using regular expressions.</p>
 * @see java.util.regex.Pattern
 * @since 3.8
 */
public class RegExUtils {

    /**
     * <p>Removes each substring of the text String that matches the given regular expression pattern.</p>
     *
     * This method is a {@code null} safe equivalent to:
     * <ul>
     *  <li>{@code pattern.matcher(text).replaceAll(StringUtils.EMPTY)}</li>
     * </ul>
     *
     * <p>A {@code null} reference passed to this method is a no-op.</p>
     *
     * <pre>
     * StringUtils.removeAll(null, *)      = null
     * StringUtils.removeAll("any", (Pattern) null)  = "any"
     * StringUtils.removeAll("any", Pattern.compile(""))    = "any"
     * StringUtils.removeAll("any", Pattern.compile(".*"))  = ""
     * StringUtils.removeAll("any", Pattern.compile(".+"))  = ""
     * StringUtils.removeAll("abc", Pattern.compile(".?"))  = ""
     * StringUtils.removeAll("A&lt;__&gt;\n&lt;__&gt;B", Pattern.compile("&lt;.*&gt;"))      = "A\nB"
     * StringUtils.removeAll("A&lt;__&gt;\n&lt;__&gt;B", Pattern.compile("(?s)&lt;.*&gt;"))  = "AB"
     * StringUtils.removeAll("A&lt;__&gt;\n&lt;__&gt;B", Pattern.compile("&lt;.*&gt;", Pattern.DOTALL))  = "AB"
     * StringUtils.removeAll("ABCabc123abc", Pattern.compile("[a-z]"))     = "ABC123"
     * </pre>
     *
     * @param text  text to remove from, may be null
     * @param regex  the regular expression to which this string is to be matched
     * @return  the text with any removes processed,
     *              {@code null} if null String input
     *
     * @see #replaceAll(String, Pattern, String)
     * @see java.util.regex.Matcher#replaceAll(String)
     * @see java.util.regex.Pattern
     */
    public static String removeAll(final String text, final Pattern regex) {
        return replaceAll(text, regex, StringUtils.EMPTY);
    }

    /**
     * <p>Removes each substring of the text String that matches the given regular expression.</p>
     *
     * This method is a {@code null} safe equivalent to:
     * <ul>
     *  <li>{@code text.replaceAll(regex, StringUtils.EMPTY)}</li>
     *  <li>{@code Pattern.compile(regex).matcher(text).replaceAll(StringUtils.EMPTY)}</li>
     * </ul>
     *
     * <p>A {@code null} reference passed to this method is a no-op.</p>
     *
     * <p>Unlike in the {@link #removePattern(String, String)} method, the {@link Pattern#DOTALL} option
     * is NOT automatically added.
     * To use the DOTALL option prepend <code>"(?s)"</code> to the regex.
     * DOTALL is also known as single-line mode in Perl.</p>
     *
     * <pre>
     * StringUtils.removeAll(null, *)      = null
     * StringUtils.removeAll("any", (String) null)  = "any"
     * StringUtils.removeAll("any", "")    = "any"
     * StringUtils.removeAll("any", ".*")  = ""
     * StringUtils.removeAll("any", ".+")  = ""
     * StringUtils.removeAll("abc", ".?")  = ""
     * StringUtils.removeAll("A&lt;__&gt;\n&lt;__&gt;B", "&lt;.*&gt;")      = "A\nB"
     * StringUtils.removeAll("A&lt;__&gt;\n&lt;__&gt;B", "(?s)&lt;.*&gt;")  = "AB"
     * StringUtils.removeAll("ABCabc123abc", "[a-z]")     = "ABC123"
     * </pre>
     *
     * @param text  text to remove from, may be null
     * @param regex  the regular expression to which this string is to be matched
     * @return  the text with any removes processed,
     *              {@code null} if null String input
     *
     * @throws  java.util.regex.PatternSyntaxException
     *              if the regular expression's syntax is invalid
     *
     * @see #replaceAll(String, String, String)
     * @see #removePattern(String, String)
     * @see String#replaceAll(String, String)
     * @see java.util.regex.Pattern
     * @see java.util.regex.Pattern#DOTALL
     */
    public static String removeAll(final String text, final String regex) {
        return replaceAll(text, regex, StringUtils.EMPTY);
    }

    /**
     * <p>Removes the first substring of the text string that matches the given regular expression pattern.</p>
     *
     * This method is a {@code null} safe equivalent to:
     * <ul>
     *  <li>{@code pattern.matcher(text).replaceFirst(StringUtils.EMPTY)}</li>
     * </ul>
     *
     * <p>A {@code null} reference passed to this method is a no-op.</p>
     *
     * <pre>
     * StringUtils.removeFirst(null, *)      = null
     * StringUtils.removeFirst("any", (Pattern) null)  = "any"
     * StringUtils.removeFirst("any", Pattern.compile(""))    = "any"
     * StringUtils.removeFirst("any", Pattern.compile(".*"))  = ""
     * StringUtils.removeFirst("any", Pattern.compile(".+"))  = ""
     * StringUtils.removeFirst("abc", Pattern.compile(".?"))  = "bc"
     * StringUtils.removeFirst("A&lt;__&gt;\n&lt;__&gt;B", Pattern.compile("&lt;.*&gt;"))      = "A\n&lt;__&gt;B"
     * StringUtils.removeFirst("A&lt;__&gt;\n&lt;__&gt;B", Pattern.compile("(?s)&lt;.*&gt;"))  = "AB"
     * StringUtils.removeFirst("ABCabc123", Pattern.compile("[a-z]"))          = "ABCbc123"
     * StringUtils.removeFirst("ABCabc123abc", Pattern.compile("[a-z]+"))      = "ABC123abc"
     * </pre>
     *
     * @param text  text to remove from, may be null
     * @param regex  the regular expression pattern to which this string is to be matched
     * @return  the text with the first replacement processed,
     *              {@code null} if null String input
     *
     * @see #replaceFirst(String, Pattern, String)
     * @see java.util.regex.Matcher#replaceFirst(String)
     * @see java.util.regex.Pattern
     */
    public static String removeFirst(final String text, final Pattern regex) {
        return replaceFirst(text, regex, StringUtils.EMPTY);
    }

    /**
     * <p>Removes the first substring of the text string that matches the given regular expression.</p>
     *
     * This method is a {@code null} safe equivalent to:
     * <ul>
     *  <li>{@code text.replaceFirst(regex, StringUtils.EMPTY)}</li>
     *  <li>{@code Pattern.compile(regex).matcher(text).replaceFirst(StringUtils.EMPTY)}</li>
     * </ul>
     *
     * <p>A {@code null} reference passed to this method is a no-op.</p>
     *
     * <p>The {@link Pattern#DOTALL} option is NOT automatically added.
     * To use the DOTALL option prepend <code>"(?s)"</code> to the regex.
     * DOTALL is also known as single-line mode in Perl.</p>
     *
     * <pre>
     * StringUtils.removeFirst(null, *)      = null
     * StringUtils.removeFirst("any", (String) null)  = "any"
     * StringUtils.removeFirst("any", "")    = "any"
     * StringUtils.removeFirst("any", ".*")  = ""
     * StringUtils.removeFirst("any", ".+")  = ""
     * StringUtils.removeFirst("abc", ".?")  = "bc"
     * StringUtils.removeFirst("A&lt;__&gt;\n&lt;__&gt;B", "&lt;.*&gt;")      = "A\n&lt;__&gt;B"
     * StringUtils.removeFirst("A&lt;__&gt;\n&lt;__&gt;B", "(?s)&lt;.*&gt;")  = "AB"
     * StringUtils.removeFirst("ABCabc123", "[a-z]")          = "ABCbc123"
     * StringUtils.removeFirst("ABCabc123abc", "[a-z]+")      = "ABC123abc"
     * </pre>
     *
     * @param text  text to remove from, may be null
     * @param regex  the regular expression to which this string is to be matched
     * @return  the text with the first replacement processed,
     *              {@code null} if null String input
     *
     * @throws  java.util.regex.PatternSyntaxException
     *              if the regular expression's syntax is invalid
     *
     * @see #replaceFirst(String, String, String)
     * @see String#replaceFirst(String, String)
     * @see java.util.regex.Pattern
     * @see java.util.regex.Pattern#DOTALL
     */
    public static String removeFirst(final String text, final String regex) {
        return replaceFirst(text, regex, StringUtils.EMPTY);
    }

    /**
     * <p>Removes each substring of the source String that matches the given regular expression using the DOTALL option.</p>
     *
     * This call is a {@code null} safe equivalent to:
     * <ul>
     * <li>{@code text.replaceAll(&quot;(?s)&quot; + regex, StringUtils.EMPTY)}</li>
     * <li>{@code Pattern.compile(regex, Pattern.DOTALL).matcher(text).replaceAll(StringUtils.EMPTY)}</li>
     * </ul>
     *
     * <p>A {@code null} reference passed to this method is a no-op.</p>
     *
     * <pre>
     * StringUtils.removePattern(null, *)       = null
     * StringUtils.removePattern("any", (String) null)   = "any"
     * StringUtils.removePattern("A&lt;__&gt;\n&lt;__&gt;B", "&lt;.*&gt;")  = "AB"
     * StringUtils.removePattern("ABCabc123", "[a-z]")    = "ABC123"
     * </pre>
     *
     * @param text
     *            the source string
     * @param regex
     *            the regular expression to which this string is to be matched
     * @return The resulting {@code String}
     * @see #replacePattern(String, String, String)
     * @see String#replaceAll(String, String)
     * @see Pattern#DOTALL
     */
    public static String removePattern(final String text, final String regex) {
        return replacePattern(text, regex, StringUtils.EMPTY);
    }

    /**
     * <p>Replaces each substring of the text String that matches the given regular expression pattern with the given replacement.</p>
     *
     * This method is a {@code null} safe equivalent to:
     * <ul>
     *  <li>{@code pattern.matcher(text).replaceAll(replacement)}</li>
     * </ul>
     *
     * <p>A {@code null} reference passed to this method is a no-op.</p>
     *
     * <pre>
     * StringUtils.replaceAll(null, *, *)       = null
     * StringUtils.replaceAll("any", (Pattern) null, *)   = "any"
     * StringUtils.replaceAll("any", *, null)   = "any"
     * StringUtils.replaceAll("", Pattern.compile(""), "zzz")    = "zzz"
     * StringUtils.replaceAll("", Pattern.compile(".*"), "zzz")  = "zzz"
     * StringUtils.replaceAll("", Pattern.compile(".+"), "zzz")  = ""
     * StringUtils.replaceAll("abc", Pattern.compile(""), "ZZ")  = "ZZaZZbZZcZZ"
     * StringUtils.replaceAll("&lt;__&gt;\n&lt;__&gt;", Pattern.compile("&lt;.*&gt;"), "z")                 = "z\nz"
     * StringUtils.replaceAll("&lt;__&gt;\n&lt;__&gt;", Pattern.compile("&lt;.*&gt;", Pattern.DOTALL), "z") = "z"
     * StringUtils.replaceAll("&lt;__&gt;\n&lt;__&gt;", Pattern.compile("(?s)&lt;.*&gt;"), "z")             = "z"
     * StringUtils.replaceAll("ABCabc123", Pattern.compile("[a-z]"), "_")       = "ABC___123"
     * StringUtils.replaceAll("ABCabc123", Pattern.compile("[^A-Z0-9]+"), "_")  = "ABC_123"
     * StringUtils.replaceAll("ABCabc123", Pattern.compile("[^A-Z0-9]+"), "")   = "ABC123"
     * StringUtils.replaceAll("Lorem ipsum  dolor   sit", Pattern.compile("( +)([a-z]+)"), "_$2")  = "Lorem_ipsum_dolor_sit"
     * </pre>
     *
     * @param text  text to search and replace in, may be null
     * @param regex  the regular expression pattern to which this string is to be matched
     * @param replacement  the string to be substituted for each match
     * @return  the text with any replacements processed,
     *              {@code null} if null String input
     *
     * @see java.util.regex.Matcher#replaceAll(String)
     * @see java.util.regex.Pattern
     */
    public static String replaceAll(final String text, final Pattern regex, final String replacement) {
        if (text == null || regex == null || replacement == null) {
            return text;
        }
        return regex.matcher(text).replaceAll(replacement);
    }

    /**
     * <p>Replaces each substring of the text String that matches the given regular expression
     * with the given replacement.</p>
     *
     * This method is a {@code null} safe equivalent to:
     * <ul>
     *  <li>{@code text.replaceAll(regex, replacement)}</li>
     *  <li>{@code Pattern.compile(regex).matcher(text).replaceAll(replacement)}</li>
     * </ul>
     *
     * <p>A {@code null} reference passed to this method is a no-op.</p>
     *
     * <p>Unlike in the {@link #replacePattern(String, String, String)} method, the {@link Pattern#DOTALL} option
     * is NOT automatically added.
     * To use the DOTALL option prepend <code>"(?s)"</code> to the regex.
     * DOTALL is also known as single-line mode in Perl.</p>
     *
     * <pre>
     * StringUtils.replaceAll(null, *, *)       = null
     * StringUtils.replaceAll("any", (String) null, *)   = "any"
     * StringUtils.replaceAll("any", *, null)   = "any"
     * StringUtils.replaceAll("", "", "zzz")    = "zzz"
     * StringUtils.replaceAll("", ".*", "zzz")  = "zzz"
     * StringUtils.replaceAll("", ".+", "zzz")  = ""
     * StringUtils.replaceAll("abc", "", "ZZ")  = "ZZaZZbZZcZZ"
     * StringUtils.replaceAll("&lt;__&gt;\n&lt;__&gt;", "&lt;.*&gt;", "z")      = "z\nz"
     * StringUtils.replaceAll("&lt;__&gt;\n&lt;__&gt;", "(?s)&lt;.*&gt;", "z")  = "z"
     * StringUtils.replaceAll("ABCabc123", "[a-z]", "_")       = "ABC___123"
     * StringUtils.replaceAll("ABCabc123", "[^A-Z0-9]+", "_")  = "ABC_123"
     * StringUtils.replaceAll("ABCabc123", "[^A-Z0-9]+", "")   = "ABC123"
     * StringUtils.replaceAll("Lorem ipsum  dolor   sit", "( +)([a-z]+)", "_$2")  = "Lorem_ipsum_dolor_sit"
     * </pre>
     *
     * @param text  text to search and replace in, may be null
     * @param regex  the regular expression to which this string is to be matched
     * @param replacement  the string to be substituted for each match
     * @return  the text with any replacements processed,
     *              {@code null} if null String input
     *
     * @throws  java.util.regex.PatternSyntaxException
     *              if the regular expression's syntax is invalid
     *
     * @see #replacePattern(String, String, String)
     * @see String#replaceAll(String, String)
     * @see java.util.regex.Pattern
     * @see java.util.regex.Pattern#DOTALL
     */
    public static String replaceAll(final String text, final String regex, final String replacement) {
        if (text == null || regex == null || replacement == null) {
            return text;
        }
        return text.replaceAll(regex, replacement);
    }

    /**
     * <p>Replaces the first substring of the text string that matches the given regular expression pattern
     * with the given replacement.</p>
     *
     * This method is a {@code null} safe equivalent to:
     * <ul>
     *  <li>{@code pattern.matcher(text).replaceFirst(replacement)}</li>
     * </ul>
     *
     * <p>A {@code null} reference passed to this method is a no-op.</p>
     *
     * <pre>
     * StringUtils.replaceFirst(null, *, *)       = null
     * StringUtils.replaceFirst("any", (Pattern) null, *)   = "any"
     * StringUtils.replaceFirst("any", *, null)   = "any"
     * StringUtils.replaceFirst("", Pattern.compile(""), "zzz")    = "zzz"
     * StringUtils.replaceFirst("", Pattern.compile(".*"), "zzz")  = "zzz"
     * StringUtils.replaceFirst("", Pattern.compile(".+"), "zzz")  = ""
     * StringUtils.replaceFirst("abc", Pattern.compile(""), "ZZ")  = "ZZabc"
     * StringUtils.replaceFirst("&lt;__&gt;\n&lt;__&gt;", Pattern.compile("&lt;.*&gt;"), "z")      = "z\n&lt;__&gt;"
     * StringUtils.replaceFirst("&lt;__&gt;\n&lt;__&gt;", Pattern.compile("(?s)&lt;.*&gt;"), "z")  = "z"
     * StringUtils.replaceFirst("ABCabc123", Pattern.compile("[a-z]"), "_")          = "ABC_bc123"
     * StringUtils.replaceFirst("ABCabc123abc", Pattern.compile("[^A-Z0-9]+"), "_")  = "ABC_123abc"
     * StringUtils.replaceFirst("ABCabc123abc", Pattern.compile("[^A-Z0-9]+"), "")   = "ABC123abc"
     * StringUtils.replaceFirst("Lorem ipsum  dolor   sit", Pattern.compile("( +)([a-z]+)"), "_$2")  = "Lorem_ipsum  dolor   sit"
     * </pre>
     *
     * @param text  text to search and replace in, may be null
     * @param regex  the regular expression pattern to which this string is to be matched
     * @param replacement  the string to be substituted for the first match
     * @return  the text with the first replacement processed,
     *              {@code null} if null String input
     *
     * @see java.util.regex.Matcher#replaceFirst(String)
     * @see java.util.regex.Pattern
     */
    public static String replaceFirst(final String text, final Pattern regex, final String replacement) {
        if (text == null || regex == null|| replacement == null ) {
            return text;
        }
        return regex.matcher(text).replaceFirst(replacement);
    }

    /**
     * <p>Replaces the first substring of the text string that matches the given regular expression
     * with the given replacement.</p>
     *
     * This method is a {@code null} safe equivalent to:
     * <ul>
     *  <li>{@code text.replaceFirst(regex, replacement)}</li>
     *  <li>{@code Pattern.compile(regex).matcher(text).replaceFirst(replacement)}</li>
     * </ul>
     *
     * <p>A {@code null} reference passed to this method is a no-op.</p>
     *
     * <p>The {@link Pattern#DOTALL} option is NOT automatically added.
     * To use the DOTALL option prepend <code>"(?s)"</code> to the regex.
     * DOTALL is also known as single-line mode in Perl.</p>
     *
     * <pre>
     * StringUtils.replaceFirst(null, *, *)       = null
     * StringUtils.replaceFirst("any", (String) null, *)   = "any"
     * StringUtils.replaceFirst("any", *, null)   = "any"
     * StringUtils.replaceFirst("", "", "zzz")    = "zzz"
     * StringUtils.replaceFirst("", ".*", "zzz")  = "zzz"
     * StringUtils.replaceFirst("", ".+", "zzz")  = ""
     * StringUtils.replaceFirst("abc", "", "ZZ")  = "ZZabc"
     * StringUtils.replaceFirst("&lt;__&gt;\n&lt;__&gt;", "&lt;.*&gt;", "z")      = "z\n&lt;__&gt;"
     * StringUtils.replaceFirst("&lt;__&gt;\n&lt;__&gt;", "(?s)&lt;.*&gt;", "z")  = "z"
     * StringUtils.replaceFirst("ABCabc123", "[a-z]", "_")          = "ABC_bc123"
     * StringUtils.replaceFirst("ABCabc123abc", "[^A-Z0-9]+", "_")  = "ABC_123abc"
     * StringUtils.replaceFirst("ABCabc123abc", "[^A-Z0-9]+", "")   = "ABC123abc"
     * StringUtils.replaceFirst("Lorem ipsum  dolor   sit", "( +)([a-z]+)", "_$2")  = "Lorem_ipsum  dolor   sit"
     * </pre>
     *
     * @param text  text to search and replace in, may be null
     * @param regex  the regular expression to which this string is to be matched
     * @param replacement  the string to be substituted for the first match
     * @return  the text with the first replacement processed,
     *              {@code null} if null String input
     *
     * @throws  java.util.regex.PatternSyntaxException
     *              if the regular expression's syntax is invalid
     *
     * @see String#replaceFirst(String, String)
     * @see java.util.regex.Pattern
     * @see java.util.regex.Pattern#DOTALL
     */
    public static String replaceFirst(final String text, final String regex, final String replacement) {
        if (text == null || regex == null|| replacement == null ) {
            return text;
        }
        return text.replaceFirst(regex, replacement);
    }

    /**
     * <p>Replaces each substring of the source String that matches the given regular expression with the given
     * replacement using the {@link Pattern#DOTALL} option. DOTALL is also known as single-line mode in Perl.</p>
     *
     * This call is a {@code null} safe equivalent to:
     * <ul>
     * <li>{@code text.replaceAll(&quot;(?s)&quot; + regex, replacement)}</li>
     * <li>{@code Pattern.compile(regex, Pattern.DOTALL).matcher(text).replaceAll(replacement)}</li>
     * </ul>
     *
     * <p>A {@code null} reference passed to this method is a no-op.</p>
     *
     * <pre>
     * StringUtils.replacePattern(null, *, *)       = null
     * StringUtils.replacePattern("any", (String) null, *)   = "any"
     * StringUtils.replacePattern("any", *, null)   = "any"
     * StringUtils.replacePattern("", "", "zzz")    = "zzz"
     * StringUtils.replacePattern("", ".*", "zzz")  = "zzz"
     * StringUtils.replacePattern("", ".+", "zzz")  = ""
     * StringUtils.replacePattern("&lt;__&gt;\n&lt;__&gt;", "&lt;.*&gt;", "z")       = "z"
     * StringUtils.replacePattern("ABCabc123", "[a-z]", "_")       = "ABC___123"
     * StringUtils.replacePattern("ABCabc123", "[^A-Z0-9]+", "_")  = "ABC_123"
     * StringUtils.replacePattern("ABCabc123", "[^A-Z0-9]+", "")   = "ABC123"
     * StringUtils.replacePattern("Lorem ipsum  dolor   sit", "( +)([a-z]+)", "_$2")  = "Lorem_ipsum_dolor_sit"
     * </pre>
     *
     * @param text
     *            the source string
     * @param regex
     *            the regular expression to which this string is to be matched
     * @param replacement
     *            the string to be substituted for each match
     * @return The resulting {@code String}
     * @see #replaceAll(String, String, String)
     * @see String#replaceAll(String, String)
     * @see Pattern#DOTALL
     */
    public static String replacePattern(final String text, final String regex, final String replacement) {
        if (text == null || regex == null || replacement == null) {
            return text;
        }
        return Pattern.compile(regex, Pattern.DOTALL).matcher(text).replaceAll(replacement);
    }

}