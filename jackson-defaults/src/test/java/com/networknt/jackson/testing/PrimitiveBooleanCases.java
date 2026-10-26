package com.networknt.jackson.testing;

import com.networknt.jackson.JacksonDefaults;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.List;
import java.util.function.Predicate;

/** Shared, test-only corpus for JVM and native config binding checks. */
public final class PrimitiveBooleanCases {
    private PrimitiveBooleanCases() { }

    public enum Outcome {
        TRUE, FALSE, INVALID_FORMAT, TYPE_MISMATCH;

        public boolean rejects() {
            return this == INVALID_FORMAT || this == TYPE_MISMATCH;
        }
    }

    private enum Shape { TRUE, FALSE, NULL, EMPTY_STRING, INVALID_FORMAT, TYPE_MISMATCH }

    public record Input(String name, String json, Shape shape) {
        public Outcome expected(boolean strict, boolean defaultValue, boolean skipNull) {
            return switch (shape) {
                case TRUE -> Outcome.TRUE;
                case FALSE -> Outcome.FALSE;
                case NULL -> skipNull ? (defaultValue ? Outcome.TRUE : Outcome.FALSE)
                        : strict ? Outcome.TYPE_MISMATCH : Outcome.FALSE;
                case EMPTY_STRING -> strict ? Outcome.TYPE_MISMATCH : Outcome.FALSE;
                case INVALID_FORMAT -> Outcome.INVALID_FORMAT;
                case TYPE_MISMATCH -> Outcome.TYPE_MISMATCH;
            };
        }
    }

    public static final List<Input> INPUTS = List.of(
            new Input("true", "true", Shape.TRUE),
            new Input("false", "false", Shape.FALSE),
            new Input("null", "null", Shape.NULL),
            new Input("integer one", "1", Shape.TRUE),
            new Input("integer zero", "0", Shape.FALSE),
            new Input("integer two", "2", Shape.TRUE),
            new Input("string true", "\"true\"", Shape.TRUE),
            new Input("string false", "\"false\"", Shape.FALSE),
            new Input("string yes", "\"yes\"", Shape.INVALID_FORMAT),
            new Input("array", "[]", Shape.TYPE_MISMATCH),
            new Input("empty string", "\"\"", Shape.EMPTY_STRING),
            new Input("string one", "\"1\"", Shape.INVALID_FORMAT),
            new Input("decimal zero", "0.0", Shape.TYPE_MISMATCH));

    public record Totals(int cases, int defaults) {
        public Totals plus(Totals other) {
            return new Totals(cases + other.cases, defaults + other.defaults);
        }
    }

    public static <T> Totals verify(Class<T> type, String property, Predicate<T> getter,
                                     boolean defaultValue, boolean skipNull,
                                     boolean ignoreUnrelatedEmptyBeans) {
        int cases = 0;
        int defaults = 0;
        for (boolean strict : new boolean[]{false, true}) {
            var mapper = JacksonDefaults.builder()
                    .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, strict).build();
            if (getter.test(mapper.readValue("{}", type)) != defaultValue) {
                throw new AssertionError(type.getSimpleName() + ": omitted default changed");
            }
            defaults++;
            var writer = ignoreUnrelatedEmptyBeans
                    ? mapper.writer().without(SerializationFeature.FAIL_ON_EMPTY_BEANS) : mapper.writer();
            for (Input input : INPUTS) {
                Outcome expected = input.expected(strict, defaultValue, skipNull);
                String context = type.getSimpleName() + "." + property + ": " + input.name() + ", strict=" + strict;
                String document = "{\"" + property + "\":" + input.json() + "}";
                if (expected.rejects()) {
                    try {
                        mapper.readValue(document, type);
                        throw new AssertionError("Expected rejection: " + context);
                    } catch (MismatchedInputException error) {
                        Class<?> expectedType = expected == Outcome.INVALID_FORMAT
                                ? InvalidFormatException.class : MismatchedInputException.class;
                        if (error.getClass() != expectedType) {
                            throw new AssertionError("Wrong rejection: " + context, error);
                        }
                    }
                } else {
                    T value = mapper.readValue(document, type);
                    boolean expectedValue = expected == Outcome.TRUE;
                    if (getter.test(value) != expectedValue) {
                        throw new AssertionError("Wrong binding: " + context);
                    }
                    var output = mapper.readTree(writer.writeValueAsString(value));
                    if (!output.has(property) || output.get(property).booleanValue() != expectedValue) {
                        throw new AssertionError("Wrong serialization: " + context);
                    }
                }
                cases++;
            }
        }
        return new Totals(cases, defaults);
    }
}
