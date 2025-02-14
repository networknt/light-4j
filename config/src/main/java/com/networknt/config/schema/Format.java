package com.networknt.config.schema;

/**
 * Enumerates the possible formats for a JSON Schema.
 */
public enum Format {
    date_time("date-time"),
    date("date"),
    time("time"),
    duration("duration"),
    email("email"),
    idn_email("idn-email"),
    hostname("hostname"),
    idn_hostname("idn-hostname"),
    ipv4("ipv4"),
    ipv6("ipv6"),
    uri("uri"),
    uri_reference("uri-reference"),
    iri("iri"),
    iri_reference("iri-reference"),
    uuid("uuid"),
    uri_template("uri-template"),
    json_pointer("json-pointer"),
    relative_json_pointer("relative-json-pointer"),
    regex("regex"),
    float32("float"),
    float64("float64"),
    float128("float128"),
    int32("int32"),
    int64("int64"),
    int128("int128"),
    u32("u32"),
    u64("u64"),
    u128("u128"),
    none("none");


    private final String format;

    Format(final String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return format;
    }

}
