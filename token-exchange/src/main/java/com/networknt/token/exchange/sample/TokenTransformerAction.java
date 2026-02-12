package com.networknt.token.exchange.sample;


import com.networknt.token.exchange.RequestContext;
import com.networknt.token.exchange.TokenExchangeConfig;
import com.networknt.token.exchange.TokenExchangeService;
import com.networknt.token.exchange.extract.ClientIdentityExtractorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

import static com.networknt.utility.Constants.ERROR_MESSAGE;

/**
 * Rule engine plugin for token transformation.
 * This plugin resolves token schemas based on the action value provided by the rule engine.
 * 
 * <p>For automatic schema resolution based on client ID, use the value "auto" in the action.</p>
 * 
 * <p>The actual token transformation logic is delegated to {@link }
 * which can be used independently of the rule engine.</p>
 *
 * @author Kalev Gonvick
 */
public class TokenTransformerAction {

    private static final Logger LOG = LoggerFactory.getLogger(TokenTransformerAction.class);
    private static final TokenExchangeConfig CONFIG = TokenExchangeConfig.load();

    private final TokenExchangeService transformationService;

    public TokenTransformerAction() {
        LOG.trace("Constructing token-transformer plugin");
        this.transformationService = new TokenExchangeService();
    }


    public void performAction(String ruleId, String actionId, final Map<String, Object> objMap,
                              final Map<String, Object> resultMap, final Collection<String[]> actionValues) {
        LOG.trace("TokenTransformer plugin starts with ruleId: {} actionId: {}", ruleId, actionId);

        final var requestContext = RequestContext.fromObjectMap(objMap);

        for (final var actionValue : actionValues) {
            if (actionValue[0].equals(TokenExchangeConfig.TOKEN_SCHEMA)) {
                try {
                    String schemaName = actionValue[1];

                    // If schema name is "auto" or empty, use request-based resolution
                    if (schemaName == null || schemaName.isEmpty() || "auto".equalsIgnoreCase(schemaName)) {
                        transformationService.transformByRequestContext(requestContext, resultMap);
                    } else {
                        // Use explicit schema name
                        transformationService.transformBySchemaName(schemaName, resultMap, requestContext);
                    }

                } catch (Exception e) {
                    LOG.error("Exception occurred while processing token schema '{}'", actionValue[1], e);
                    Thread.currentThread().interrupt();
                    resultMap.put(ERROR_MESSAGE, e.getMessage());
                    return;
                }
            }
        }

        LOG.trace("TokenTransformer plugin ends.");
    }
}
