package com.networknt.config.schema;

/**
 * Enumerates the possible formats for a JSON Schema.
 */
public enum Format {
    DATE_TIME("date-time"),
    DATE("date"),
    TIME("time"),
    DURATION("duration"),
    EMAIL("email"),
    IDN_EMAIL("idn-email"),
    HOSTNAME("hostname"),
    IDN_HOSTNAME("idn-hostname"),
    IPV4("ipv4"),
    IPV6("ipv6"),
    URI("uri"),
    URI_REFERENCE("uri-reference"),
    IRI("iri"),
    IRI_REFERENCE("iri-reference"),
    UUID("uuid"),
    URI_TEMPLATE("uri-template"),
    JSON_POINTER("json-pointer"),
    RELATIVE_JSON_POINTER("relative-json-pointer"),
    REGEX("regex"),
    FLOAT32("float"),
    FLOAT64("float64"),
    FLOAT128("float128"),
    INT32("int32"),
    INT64("int64"),
    INT128("int128"),
    U32("u32"),
    U64("u64"),
    U128("u128"),
    NONE("none");


    private final String format;

    Format(final String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return format;
    }

}
