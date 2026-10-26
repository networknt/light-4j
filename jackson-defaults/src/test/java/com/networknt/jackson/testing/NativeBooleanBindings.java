package com.networknt.jackson.testing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.networknt.jackson.JacksonDefaults;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/** Fails on newly added Jackson-bound booleans without a method and native entry. */
public final class NativeBooleanBindings {
    private NativeBooleanBindings() { }

    public static void verify(Class<?> type, String resource) throws IOException {
        try (var stream = type.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new AssertionError("Missing native metadata: " + resource);
            }
            verify(type, JacksonDefaults.mapper().readTree(stream));
        }
    }

    public static void verify(Class<?> type, JsonNode metadata) {
        for (var field : type.getDeclaredFields()) {
            if (Modifier.isPrivate(field.getModifiers()) && field.getType().isPrimitive()
                    && field.isAnnotationPresent(JsonProperty.class)
                    && field.getType() != boolean.class && field.getType() != int.class) {
                throw new AssertionError("Unqualified primitive binding type: " + field);
            }
        }
        var booleans = Arrays.stream(type.getDeclaredFields())
                .filter(field -> Modifier.isPrivate(field.getModifiers()) && field.getType() == boolean.class
                        && field.isAnnotationPresent(JsonProperty.class)).toList();
        if (booleans.isEmpty()) {
            throw new AssertionError("No annotated primitive booleans: " + type.getName());
        }
        JsonNode entry = null;
        for (JsonNode candidate : metadata) {
            if (type.getName().equals(candidate.path("name").asString())) {
                if (entry != null) throw new AssertionError("Duplicate native class: " + type.getName());
                entry = candidate;
            }
        }
        if (entry == null) throw new AssertionError("Missing native class: " + type.getName());
        for (var field : booleans) {
            String name = field.getAnnotation(JsonProperty.class).value();
            if (name.isEmpty()) name = field.getName();
            String property = name;
            var setters = Arrays.stream(type.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(JsonSetter.class)
                            && property.equals(method.getAnnotation(JsonSetter.class).value())).toList();
            if (setters.size() != 1) {
                throw new AssertionError("Require one explicit setter: " + type.getName() + "." + property);
            }
            var setter = setters.getFirst();
            if (!Modifier.isPrivate(setter.getModifiers()) || setter.getReturnType() != void.class
                    || !Arrays.equals(setter.getParameterTypes(), new Class<?>[]{boolean.class})) {
                throw new AssertionError("Require private primitive setter: " + setter.getName());
            }
        }
        for (var setter : type.getDeclaredMethods()) {
            if (!setter.isAnnotationPresent(JsonSetter.class)) continue;
            String property = setter.getAnnotation(JsonSetter.class).value();
            boolean fieldExists = booleans.stream().anyMatch(field -> {
                String name = field.getAnnotation(JsonProperty.class).value();
                return property.equals(name.isEmpty() ? field.getName() : name);
            });
            if (!fieldExists) throw new AssertionError("Setter lacks a boolean field: " + setter.getName());
            boolean registered = false;
            for (JsonNode method : entry.path("methods")) {
                JsonNode parameters = method.path("parameterTypes");
                if (setter.getName().equals(method.path("name").asString()) && parameters.size() == 1
                        && "boolean".equals(parameters.get(0).asString())) registered = true;
            }
            if (!registered) throw new AssertionError("Missing native setter: " + setter.getName());
        }
    }
}
