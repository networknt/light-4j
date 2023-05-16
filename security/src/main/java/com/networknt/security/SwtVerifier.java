package com.networknt.security;

import com.networknt.client.ClientConfig;
import com.networknt.client.oauth.TokenInfo;
import com.networknt.client.oauth.TokenIntrospectionRequest;
import com.networknt.client.oauth.OauthHelper;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.monad.Success;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * This is common part the simple web token verification class. It will be called by the UnifiedSecurityHandler
 * or SwtVerifyHandler in light-rest-4j.
 * @author Steve Hu
 */
public class SwtVerifier extends TokenVerifier {
    static final Logger logger = LoggerFactory.getLogger(SwtVerifier.class);
    public static final String OAUTH_INTROSPECTION_ERROR = "ERR10079";
    public static final String TOKEN_INFO_ERROR = "ERR10080";
    public static final String INTROSPECTED_TOKEN_EXPIRED = "ERR10081";
    static SecurityConfig config;

    public SwtVerifier(SecurityConfig config) {
        this.config = config;
        if(logger.isInfoEnabled()) logger.info("SwtVerifier is constructed.");
    }

    /**
     * This method is to keep backward compatible for those call without VerificationKeyResolver. The single
     * auth server is used in this case.
     *
     * @param swt          SWT Simple Web Token
     * @param requestPath  request path
     * @param swtServiceIds A list of serviceIds from the UnifiedSecurityHandler
     * @return {@link Result} of {@link TokenInfo}.
     */
    public Result<TokenInfo> verifySwt(String swt, String requestPath, List<String> swtServiceIds) {
        // based on the pathPrefix to find the serviceId, based on the serviceId to find the introspection configuration
        return getTokenInfoForToken(swt, swtServiceIds != null ? swtServiceIds : requestPath);
    }

    /**
     * Retrieve token info from an oauth server introspection endpoint with the swt. This method is used when a new swt
     * is received. It will look up the key service by request path or serviceId  first.
     *
     * @param swt         String of simple web token
     * @param requestPathOrSwtServiceIds String of request path or list of strings for swtServiceIds
     * @return {@link Result} of {@link TokenInfo}.
     */
    @SuppressWarnings("unchecked")
    private Result<TokenInfo> getTokenInfoForToken(String swt, Object requestPathOrSwtServiceIds) {
        if (logger.isTraceEnabled()) {
            logger.trace("swt = " + swt + requestPathOrSwtServiceIds instanceof String ? " requestPath = " + requestPathOrSwtServiceIds : " swtServiceIds = " + requestPathOrSwtServiceIds);
        }
        ClientConfig clientConfig = ClientConfig.get();
        Result<TokenInfo> result = null;
        Map<String, Object> config;

        if (requestPathOrSwtServiceIds != null && clientConfig.isMultipleAuthServers()) {
            if(requestPathOrSwtServiceIds instanceof String) {
                String requestPath = (String)requestPathOrSwtServiceIds;
                Map<String, String> pathPrefixServices = clientConfig.getPathPrefixServices();
                if (pathPrefixServices == null || pathPrefixServices.size() == 0) {
                    throw new ConfigException("pathPrefixServices property is missing or has an empty value in client.yml");
                }
                // lookup the serviceId based on the full path and the prefix mapping by iteration here.
                String serviceId = null;
                for (Map.Entry<String, String> entry : pathPrefixServices.entrySet()) {
                    if (requestPath.startsWith(entry.getKey())) {
                        serviceId = entry.getValue();
                    }
                }
                if (serviceId == null) {
                    throw new ConfigException("serviceId cannot be identified in client.yml with the requestPath = " + requestPath);
                }
                config = getJwkConfig(clientConfig, serviceId);
                result = inspectToken(swt, config);
            } else if (requestPathOrSwtServiceIds instanceof List) {
                // for this particular path prefix, there are two OAuth servers set up to inspect the token. Which one is success
                // with active true will be used. Here we just return the one entry with active equal to true.
                List<String> swtServiceIds = (List<String>)requestPathOrSwtServiceIds;
                for(String serviceId: swtServiceIds) {
                    config = getJwkConfig(clientConfig, serviceId);
                    result  = inspectToken(swt, config);
                    if(result.isSuccess()) {
                        // find the first success, we need to break the loop.
                        break;
                    }
                }
                // at this moment, the last result will be return if all of them are failures.
            } else {
                throw new ConfigException("requestPathOrSwtServiceIds must be a string or a list of strings");
            }
        } else {
            // get the token introspection config from the key section in the client.yml token key.
            result = inspectToken(swt, null);
        }
        return result;
    }

    private Result<TokenInfo> inspectToken(String swt, Map<String, Object> config) {
        // get the token info with the swt token and config map.
        if (logger.isTraceEnabled() && config != null)
            logger.trace("OAuth token info introspection config = " + JsonMapper.toJson(config));
        // config is not null if isMultipleAuthServers is true. If it is null, then the key section is used from the client.yml
        TokenIntrospectionRequest introspectionRequest = new TokenIntrospectionRequest(swt, config);

        try {
            if (logger.isTraceEnabled())
                logger.trace("Getting token info from {}", introspectionRequest.getServerUrl());

            Result<String> result = OauthHelper.getIntrospection(swt, introspectionRequest);

            if (logger.isTraceEnabled())
                logger.trace("Got token info response body {} from {}", result.getResult(), introspectionRequest.getServerUrl());
            // only active is true will be converted to TokenInfo. Other responses will be converted to failure status.
            if(result.isFailure()) {
                return Failure.of(result.getError());
            } else {
                TokenInfo tokenInfo = JsonMapper.fromJson(result.getResult(), TokenInfo.class);
                if(tokenInfo.getError() != null) {
                    // error response from the introspection endpoint. .
                    return Failure.of(new Status(TOKEN_INFO_ERROR, tokenInfo.getError(), tokenInfo.getErrorDescription()));
                } else if(!tokenInfo.isActive()){
                    // token expired already.
                    return Failure.of(new Status(INTROSPECTED_TOKEN_EXPIRED, swt));
                } else {
                    // token is active and need to return the token info.
                    return Success.of(tokenInfo);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get token info - {} - {}", new Status(OAUTH_INTROSPECTION_ERROR, introspectionRequest.getServerUrl(), swt), e.getMessage(), e);
            return Failure.of(new Status(OAUTH_INTROSPECTION_ERROR, introspectionRequest.getServerUrl(), swt));
        }
    }


}
