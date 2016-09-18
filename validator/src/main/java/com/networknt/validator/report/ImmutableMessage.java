package com.networknt.validator.report;

import static java.util.Objects.requireNonNull;

class ImmutableMessage implements ValidationReport.Message {

    private final String key;
    private final String message;

    ImmutableMessage(final String key, final String message) {
        this.key = requireNonNull(key, "A key is required");
        this.message = requireNonNull(message, "A message is required");
    }

    @Override
    public String getKey() {
        return key;
    }


    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return message.replace("\n", "\n\t");
    }

}
