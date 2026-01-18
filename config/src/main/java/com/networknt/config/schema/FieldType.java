package com.networknt.config.schema;

/**
 * All supported types for schema generation.
 * Used to determine the node type and the string types to compare to.
 *
 * @author Kalev Gonvick
 */
public enum FieldType {
    INTEGER("integer"),
    NUMBER("number"),
    STRING("string"),
    OBJECT("object"),
    ARRAY("array"),
    NULL("null"),
    BOOLEAN("boolean"),
    MAP("object");


    private final String type;

    FieldType(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return this.type;
    }

    public FieldNode.Builder newBuilder(final String nodeName) {
        return new FieldNode.Builder(this, nodeName);
    }
}
