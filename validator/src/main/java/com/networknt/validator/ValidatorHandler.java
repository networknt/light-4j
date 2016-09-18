package com.networknt.validator;

import com.networknt.config.Config;
import com.networknt.security.SwaggerHelper;
import com.networknt.validator.report.MessageResolver;
import com.networknt.validator.report.MutableValidationReport;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * This is a swagger validator handler that validate request and response based on the spec. In
 * production only request validator should be turned on and response validator should only be
 * used during development.
 *
 * Created by steve on 17/09/16.
 */
public class ValidatorHandler implements HttpHandler {
    public static final String CONFIG_NAME = "validator";
    public static final String ENABLE_VALIDATOR = "enableValidator";
    static final String ENABLE_RESPONSE_VALIDATOR = "enableResponseValidator";

    static final Logger logger = LoggerFactory.getLogger(ValidatorHandler.class);

    private final HttpHandler next;

    static final MessageResolver messages = new MessageResolver();

    RequestValidator requestValidator;
    ResponseValidator responseValidator;

    public ValidatorHandler(final HttpHandler next) {

        MessageResolver messages = new MessageResolver();
        final SchemaValidator schemaValidator = new SchemaValidator(SwaggerHelper.swagger, messages);
        this.requestValidator = new RequestValidator(schemaValidator, messages);
        this.responseValidator = new ResponseValidator(schemaValidator, messages);
        this.next = next;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        ValidatorConfig config = (ValidatorConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ValidatorConfig.class);

        final MutableValidationReport validationReport = new MutableValidationReport();
        final NormalisedPath requestPath = new ApiBasedNormalisedPath(exchange.getRequestURI());

        final Optional<NormalisedPath> maybeApiPath = findMatchingApiPath(requestPath);
        if (!maybeApiPath.isPresent()) {
            validationReport.add(messages.get("validation.request.path.missing", exchange.getRequestURI()));
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(validationReport));
            return;
        }

        final NormalisedPath swaggerPathString = maybeApiPath.get();
        final Path swaggerPath = SwaggerHelper.swagger.getPath(swaggerPathString.original());

        final HttpMethod httpMethod = HttpMethod.valueOf(exchange.getRequestMethod().toString());
        final Operation operation = swaggerPath.getOperationMap().get(httpMethod);
        if (operation == null) {
            validationReport.add(messages.get("validation.request.operation.notAllowed",
                    exchange.getRequestMethod(), swaggerPathString.original()));
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(validationReport));
            return;
        }

        final SwaggerOperation swaggerOperation = new SwaggerOperation(swaggerPathString, swaggerPath, httpMethod, operation);

        validationReport.merge(requestValidator.validateRequest(requestPath, exchange, swaggerOperation));
        if(validationReport.hasErrors()) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(validationReport));
        }

        if(config.enableResponseValidator) {
            exchange.addExchangeCompleteListener(new ExchangeCompletionListener() {
                @Override
                public void exchangeEvent(final HttpServerExchange exchange, final NextListener nextListener) {
                    validationReport.merge(responseValidator.validateResponse(exchange, swaggerOperation));
                    if(validationReport.hasErrors()) {
                        logger.error("Response error" + validationReport);
                    }
                    nextListener.proceed();
                }
            });
        }

        next.handleRequest(exchange);
    }

    private Optional<NormalisedPath> findMatchingApiPath(final NormalisedPath requestPath) {
        return SwaggerHelper.swagger.getPaths().keySet()
                .stream()
                .map(p -> (NormalisedPath) new ApiBasedNormalisedPath(p))
                .filter(p -> pathMatches(requestPath, p))
                .findFirst();
    }

    private boolean pathMatches(final NormalisedPath requestPath, final NormalisedPath apiPath) {
        if (requestPath.parts().size() != apiPath.parts().size()) {
            return false;
        }
        for (int i = 0; i < requestPath.parts().size(); i++) {
            if (requestPath.part(i).equalsIgnoreCase(apiPath.part(i)) || apiPath.isParam(i)) {
                continue;
            }
            return false;
        }
        return true;
    }

    private class ApiBasedNormalisedPath implements NormalisedPath {
        private final List<String> pathParts;
        private final String original;
        private final String normalised;

        ApiBasedNormalisedPath(final String path) {
            this.original = requireNonNull(path, "A path is required");
            this.normalised = normalise(path);
            this.pathParts = unmodifiableList(asList(normalised.split("/")));
        }

        @Override
        public List<String> parts() {
            return pathParts;
        }

        @Override
        public String part(int index) {
            return pathParts.get(index);
        }

        @Override
        public boolean isParam(int index) {
            final String part = part(index);
            return part.startsWith("{") && part.endsWith("}");
        }

        @Override
        public String paramName(int index) {
            if (!isParam(index)) {
                return null;
            }
            final String part = part(index);
            return part.substring(1, part.length() - 1);
        }

        @Override
        public String original() {
            return original;
        }

        @Override
        public String normalised() {
            return normalised;
        }

        private String normalise(String requestPath) {
            if (SwaggerHelper.swagger.getBasePath() != null) {
                requestPath = requestPath.replace(SwaggerHelper.swagger.getBasePath(), "");
            }
            if (!requestPath.startsWith("/")) {
                return "/" + requestPath;
            }
            return requestPath;
        }
    }

}
