/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.networknt.utility;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class that provides fast path matching of path templates. Templates are stored in a map based on the stem of the template,
 * and matches longest stem first.
 * <p>
 * TODO: we can probably do this faster using a trie type structure, but I think the current impl should perform ok most of the time
 *
 * @author Stuart Douglas
 */
public class PathTemplateMatcher<T> {

    /**
     * Map of path template stem to the path templates that share the same base.
     */
    private final Map<String, Set<PathTemplateHolder>> pathTemplateMap = new ConcurrentHashMap<>();

    /**
     * lengths of all registered paths
     */
    private volatile int[] lengths = {};

    public PathMatchResult<T> match(final String path) {
        String normalizedPath = "".equals(path) ? "/" : path;
        if(!normalizedPath.startsWith("/"))
            normalizedPath = "/"+ normalizedPath;
        final Map<String, String> params = new LinkedHashMap<>();
        int length = normalizedPath.length();
        final int[] lengths = this.lengths;
        for (int i = 0; i < lengths.length; ++i) {
            int pathLength = lengths[i];
            if (pathLength == length) {
                Set<PathTemplateHolder> entry = pathTemplateMap.get(normalizedPath);
                if (entry != null) {
                    PathMatchResult<T> res = handleStemMatch(entry, normalizedPath, params);
                    if (res != null) {
                        return res;
                    }
                }
            } else if (pathLength < length) {
                String part = normalizedPath.substring(0, pathLength);
                Set<PathTemplateHolder> entry = pathTemplateMap.get(part);
                if (entry != null) {
                    PathMatchResult<T> res = handleStemMatch(entry, normalizedPath, params);
                    if (res != null) {
                        return res;
                    }
                }
            }
        }
        return null;
    }

    private PathMatchResult<T> handleStemMatch(Set<PathTemplateHolder> entry, final String path, final Map<String, String> params) {
        for (PathTemplateHolder val : entry) {
            if (val.template.matches(path, params)) {
                return new PathMatchResult<>(params, val.template.getTemplateString(), val.value);
            } else {
                params.clear();
            }
        }
        return null;
    }


    public synchronized PathTemplateMatcher<T> add(final PathTemplate template, final T value) {
        Set<PathTemplateHolder> values = pathTemplateMap.get(trimBase(template));
        Set<PathTemplateHolder> newValues;
        if (values == null) {
            newValues = new TreeSet<>();
        } else {
            newValues = new TreeSet<>(values);
        }
        PathTemplateHolder holder = new PathTemplateHolder(value, template);
        if (newValues.contains(holder)) {
            PathTemplate equivalent = null;
            for (PathTemplateHolder item : newValues) {
                if (item.compareTo(holder) == 0) {
                    equivalent = item.template;
                    break;
                }
            }
            throw new RuntimeException(String.format("Cannot add path template %s, matcher already contains an equivalent pattern %s", template.getTemplateString(), equivalent.getTemplateString()));
        }
        newValues.add(holder);
        pathTemplateMap.put(trimBase(template), newValues);
        buildLengths();
        return this;
    }

    private String trimBase(PathTemplate template) {
        String retval = template.getBase();

        if (template.getBase().endsWith("/") && !template.getParameterNames().isEmpty()) {
            return retval.substring(0, retval.length() - 1);
        }
        if (retval.endsWith("*")) {
            return retval.substring(0, retval.length() - 1);
        }
        return retval;
    }

    private void buildLengths() {
        final Set<Integer> lengths = new TreeSet<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return -o1.compareTo(o2);
            }
        });
        for (String p : pathTemplateMap.keySet()) {
            lengths.add(p.length());
        }

        int[] lengthArray = new int[lengths.size()];
        int pos = 0;
        for (int i : lengths) {
            lengthArray[pos++] = i; //-1 because the base paths end with a /
        }
        this.lengths = lengthArray;
    }

    public synchronized PathTemplateMatcher<T> add(final String pathTemplate, final T value) {
        final PathTemplate template = PathTemplate.create(pathTemplate);
        return add(template, value);
    }

    public synchronized PathTemplateMatcher<T> addAll(PathTemplateMatcher<T> pathTemplateMatcher) {
        for (Map.Entry<String, Set<PathTemplateHolder>> entry : pathTemplateMatcher.getPathTemplateMap().entrySet()) {
            for (PathTemplateHolder pathTemplateHolder : entry.getValue()) {
                add(pathTemplateHolder.template, pathTemplateHolder.value);
            }
        }
        return this;
    }

    Map<String, Set<PathTemplateHolder>> getPathTemplateMap() {
        return pathTemplateMap;
    }

    public Set<PathTemplate> getPathTemplates() {
        Set<PathTemplate> templates = new HashSet<>();
        for (Set<PathTemplateHolder> holders : pathTemplateMap.values()) {
            for (PathTemplateHolder holder: holders) {
                templates.add(holder.template);
            }
        }
        return templates;
    }

    public synchronized PathTemplateMatcher<T> remove(final String pathTemplate) {
        final PathTemplate template = PathTemplate.create(pathTemplate);
        return remove(template);
    }

    private synchronized PathTemplateMatcher<T> remove(PathTemplate template) {
        Set<PathTemplateHolder> values = pathTemplateMap.get(trimBase(template));
        Set<PathTemplateHolder> newValues;
        if (values == null) {
            return this;
        } else {
            newValues = new TreeSet<>(values);
        }
        Iterator<PathTemplateHolder> it = newValues.iterator();
        while (it.hasNext()) {
            PathTemplateHolder next = it.next();
            if (next.template.getTemplateString().equals(template.getTemplateString())) {
                it.remove();
                break;
            }
        }
        if (newValues.size() == 0) {
            pathTemplateMap.remove(trimBase(template));
        } else {
            pathTemplateMap.put(trimBase(template), newValues);
        }
        buildLengths();
        return this;
    }


    public synchronized T get(String template) {
        PathTemplate pathTemplate = PathTemplate.create(template);
        Set<PathTemplateHolder> values = pathTemplateMap.get(trimBase(pathTemplate));
        if(values == null) {
            return null;
        }
        for (PathTemplateHolder next : values) {
            if (next.template.getTemplateString().equals(template)) {
                return next.value;
            }
        }
        return null;
    }

    public static class PathMatchResult<T> extends PathTemplateMatch {
        private final T value;

        public PathMatchResult(Map<String, String> parameters, String matchedTemplate, T value) {
            super(matchedTemplate, parameters);
            this.value = value;
        }

        public T getValue() {
            return value;
        }
    }

    private final class PathTemplateHolder implements Comparable<PathTemplateHolder> {
        final T value;
        final PathTemplate template;

        private PathTemplateHolder(T value, PathTemplate template) {
            this.value = value;
            this.template = template;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            if (!PathTemplateHolder.class.equals(o.getClass())) return false;

            PathTemplateHolder that = (PathTemplateHolder) o;
            return template.equals(that.template);
        }

        @Override
        public int hashCode() {
            return template.hashCode();
        }

        @Override
        public int compareTo(PathTemplateHolder o) {
            return template.compareTo(o.template);
        }
    }

}
