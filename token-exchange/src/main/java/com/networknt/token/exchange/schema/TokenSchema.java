package com.networknt.token.exchange.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.config.schema.MapField;
import com.networknt.config.schema.ObjectField;


public class TokenSchema {

    private static final String SHARED_VARIABLES = "sharedVariables";
    private static final String REQUEST = "request";
    private static final String SOURCE = "source";
    private static final String UPDATE = "update";

    @ObjectField(
            configFieldName = SHARED_VARIABLES,
            description = """
                Variables populated by incoming requests and custom parsing on the response.
                Variables can be referenced using !ref().""",
            ref = SharedVariableSchema.class
    )
    @JsonProperty(SHARED_VARIABLES)
    private SharedVariableSchema sharedVariables = new SharedVariableSchema();


    @ObjectField(
            configFieldName = REQUEST,
            description = "Describes building a token service request.",
            ref = RequestSchema.class
    )
    @JsonProperty(REQUEST)
    private RequestSchema tokenRequest = new RequestSchema();

    @ObjectField(
            configFieldName = SOURCE,
            description = "Describes taking data from the token service response.",
            ref = SourceSchema.class
    )
    @JsonProperty(SOURCE)
    private SourceSchema tokenSource = new SourceSchema();

    @ObjectField(
            configFieldName = UPDATE,
            description = "Describes how the in-flight request is enriched."
    )
    @JsonProperty(UPDATE)
    private UpdateSchema tokenUpdate = new UpdateSchema();

    public SharedVariableSchema getSharedVariables() {
        return sharedVariables;
    }

    public RequestSchema getTokenRequest() {
        return tokenRequest;
    }

    public SourceSchema getTokenSource() {
        return tokenSource;
    }

    public UpdateSchema getTokenUpdate() {
        return tokenUpdate;
    }
}
