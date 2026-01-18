package com.networknt.config.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The class acts as a 'transition' layer between the data being
 * parsed from annotations and the generator implementation.
 *
 * @author Kalev Gonvick
 */
public class FieldNode {

    @JsonProperty
    final UUID id;

    @JsonProperty
    final FieldType type;

    @JsonProperty
    final String configFieldName;

    @JsonProperty
    String className;

    @JsonProperty
    String externalizedKeyName;

    @JsonProperty
    String description;

    @JsonProperty
    String defaultValue;

    @JsonProperty
    FieldNode ref;

    @JsonProperty
    List<FieldNode> refAllOf;

    @JsonProperty
    List<FieldNode> refAnyOf;

    @JsonProperty
    List<FieldNode> refOneOf;

    @JsonProperty
    Integer minInteger;

    @JsonProperty
    Integer maxInteger;

    @JsonProperty
    Integer minItems;

    @JsonProperty
    Integer maxItems;

    @JsonProperty
    Boolean exclusiveMin;

    @JsonProperty
    Boolean exclusiveMax;

    @JsonProperty
    Integer multipleOfInteger;

    @JsonProperty
    Format format;

    @JsonProperty
    String pattern;

    @JsonProperty
    Integer minLength;

    @JsonProperty
    Integer maxLength;

    @JsonProperty
    Double minNumber;

    @JsonProperty
    Double maxNumber;

    @JsonProperty
    Double multipleOfNumber;

    @JsonProperty
    Boolean uniqueItems;

    @JsonProperty
    Boolean useSubTypeDefaultValue;

    @JsonProperty
    Boolean contains;

    @JsonProperty
    List<FieldNode> childNodes;

    private FieldNode(final UUID id, final FieldType type, final String configFieldName) {
        this.type = type;
        this.id = id;
        this.configFieldName = configFieldName;
    }

    public static FieldNode defaultNode() {
        return new FieldNode(UUID.randomUUID(), FieldType.STRING, FieldType.STRING.toString());
    }

    public UUID getId() {
        return id;
    }

    public FieldType getType() {
        return type;
    }

    public String getConfigFieldName() {
        return this.configFieldName;
    }

    private <T> Optional<T> getOptional(T val) {
        return Optional.ofNullable(val);
    }

    @JsonIgnore
    public Optional<String> getExternalizedKeyName() {
        return this.getOptional(this.externalizedKeyName);
    }

    @JsonIgnore
    public Optional<String> getDescription() {
        return this.getOptional(this.description);
    }

    @JsonIgnore
    public Optional<String> getDefaultValue() {
        return this.getOptional(this.defaultValue);
    }

    @JsonIgnore
    public Optional<FieldNode> getRef() {
        return this.getOptional(this.ref);
    }

    @JsonIgnore
    public Optional<List<FieldNode>> getAllOf() {
        return this.getOptional(this.refAllOf);
    }

    @JsonIgnore
    public Optional<List<FieldNode>> getAnyOf() {
        return this.getOptional(this.refAnyOf);
    }

    @JsonIgnore
    public Optional<List<FieldNode>> getOneOf() {
        return this.getOptional(this.refOneOf);
    }

    @JsonIgnore
    public Optional<Integer> getMinInteger() {
        return this.getOptional(this.minInteger);
    }

    @JsonIgnore
    public Optional<Integer> getMaxInteger() {
        return this.getOptional(this.maxInteger);
    }

    @JsonIgnore
    public Optional<Integer> getMultipleOfInteger() {
        return this.getOptional(this.multipleOfInteger);
    }

    @JsonIgnore
    public Optional<Format> getFormat() {
        return this.getOptional(this.format);
    }

    @JsonIgnore
    public Optional<String> getPattern() {
        return this.getOptional(this.pattern);
    }

    @JsonIgnore
    public Optional<Integer> getMinLength() {
        return this.getOptional(this.minLength);
    }

    @JsonIgnore
    public Optional<Integer> getMaxLength() {
        return this.getOptional(this.maxLength);
    }

    @JsonIgnore
    public Optional<Double> getMinNumber() {
        return this.getOptional(this.minNumber);
    }

    @JsonIgnore
    public Optional<Double> getMaxNumber() {
        return this.getOptional(this.maxNumber);
    }

    @JsonIgnore
    public Optional<Double> getMultipleOfNumber() {
        return this.getOptional(this.multipleOfNumber);
    }

    @JsonIgnore
    public Optional<Boolean> getExclusiveMax() {
        return this.getOptional(this.exclusiveMin);
    }

    @JsonIgnore
    public Optional<Boolean> getExclusiveMin() {
        return this.getOptional(this.exclusiveMax);
    }

    @JsonIgnore
    public Optional<Boolean> getUnique() {
        return this.getOptional(this.uniqueItems);
    }

    @JsonIgnore
    public Optional<Boolean> getUseSubTypeDefault() {
        return this.getOptional(this.useSubTypeDefaultValue);
    }

    @JsonIgnore
    public Optional<Integer> getMinItems() {
        return this.getOptional(this.minItems);
    }

    @JsonIgnore
    public Optional<Integer> getMaxItems() {
        return this.getOptional(this.maxItems);
    }

    @JsonIgnore
    public Optional<Boolean> getContains() {
        return this.getOptional(this.contains);
    }

    @JsonIgnore
    public Optional<String> getClassName() {
        return this.getOptional(this.className);
    }

    @JsonIgnore
    public Optional<List<FieldNode>> getChildren() {
        return this.getOptional(this.childNodes);
    }


    /**
     * The builder class for FieldNode.
     * Values passed from annotations to the builder need to be checked to make
     * sure they are not in the default state for the given type.
     */
    public static class Builder {
        private final FieldNode node;
        public Builder(final FieldType type, final String configFieldName) {
            this.node = new FieldNode(UUID.randomUUID(), type, configFieldName);
        }

        public Builder externalizedKeyName(final String value) {
            if (!value.equals(ConfigSchema.DEFAULT_STRING))
                this.node.externalizedKeyName = value;
            return this;
        }

        public Builder description(final String value) {
            if (!value.equals(ConfigSchema.DEFAULT_STRING))
                this.node.description = value;
            return this;
        }

        public Builder uniqueItems(final Boolean value) {
            if (!value.equals(ConfigSchema.DEFAULT_BOOLEAN))
                this.node.uniqueItems = value;
            return this;
        }

        public Builder contains(final Boolean value) {
            if (!value.equals(ConfigSchema.DEFAULT_BOOLEAN))
                this.node.contains = value;
            return this;
        }

        public Builder subObjectDefault(final Boolean value) {
            if (!value.equals(ConfigSchema.DEFAULT_BOOLEAN))
                this.node.useSubTypeDefaultValue = value;
            return this;
        }

        public Builder defaultValue(final String value) {
            if (!value.equals(ConfigSchema.DEFAULT_STRING))
                this.node.defaultValue = value;
            return this;
        }

        public Builder min(final Integer value) {
            if (!value.equals(ConfigSchema.DEFAULT_MIN_INT))
                this.node.minInteger = value;
            return this;
        }

        public Builder ref(final FieldNode ref) {
            this.node.ref = ref;
            return this;
        }

        public Builder allOf(final List<FieldNode> value) {
            this.node.refAllOf = value;
            return this;
        }

        public Builder anyOf(final List<FieldNode> value) {
            this.node.refAnyOf = value;
            return this;
        }

        public Builder oneOf(final List<FieldNode> value) {
            this.node.refOneOf = value;
            return this;
        }

        public Builder max(final Integer value) {
            if (!value.equals(ConfigSchema.DEFAULT_MAX_INT))
                this.node.maxInteger = value;
            return this;
        }

        public Builder exclusiveMin(final Boolean value) {
            if (!value.equals(ConfigSchema.DEFAULT_BOOLEAN)) {
                this.node.exclusiveMin = value;
            }
            return this;
        }

        public Builder exclusiveMax(final Boolean value) {
            if (!value.equals(ConfigSchema.DEFAULT_BOOLEAN))
                this.node.exclusiveMax = value;
            return this;
        }

        public Builder multipleOf(final Integer value) {
            if (!value.equals(ConfigSchema.DEFAULT_INT))
                this.node.multipleOfInteger = value;
            return this;
        }

        public Builder childNodes(final List<FieldNode> value) {
            if (!value.isEmpty()) {
                this.node.childNodes = value;
            }
            return this;
        }

        public Builder minItems(final Integer value) {
            if (!value.equals(ConfigSchema.DEFAULT_INT))
                this.node.minItems = value;
            return this;
        }

        public Builder maxItems(final Integer value) {
            if (!value.equals(ConfigSchema.DEFAULT_MAX_INT))
                this.node.maxItems = value;
            return this;
        }

        public Builder format(final Format value) {
            if (!Format.none.equals(value))
                this.node.format = value;
            return this;
        }

        public Builder pattern(final String value) {
            if (!value.equals(ConfigSchema.DEFAULT_STRING))
                this.node.pattern = value;
            return this;
        }

        public Builder minLength(Integer value) {
            if (!value.equals(ConfigSchema.DEFAULT_INT))
                this.node.minLength = value;
            return this;
        }

        public Builder className(final String value) {
            if (!value.equals(ConfigSchema.DEFAULT_STRING))
                this.node.className = value;
            return this;
        }

        public Builder maxLength(final Integer value) {
            if (!value.equals(ConfigSchema.DEFAULT_MAX_INT))
                this.node.maxLength = value;
            return this;
        }

        public Builder min(final Double value) {
            if (!value.equals(ConfigSchema.DEFAULT_MIN_NUMBER))
                this.node.minNumber = value;
            return this;
        }

        public Builder max(final Double value) {
            if (!value.equals(ConfigSchema.DEFAULT_MAX_NUMBER))
                this.node.maxNumber = value;
            return this;
        }

        public Builder multipleOf(final Double value) {
            if (!value.equals(ConfigSchema.DEFAULT_NUMBER))
                this.node.multipleOfNumber = value;
            return this;
        }

        public FieldNode build() {
            return this.node;
        }

    }


}
