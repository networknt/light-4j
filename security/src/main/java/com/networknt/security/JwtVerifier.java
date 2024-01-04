/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.networknt.cache.CacheManager;
import com.networknt.client.ClientConfig;
import com.networknt.client.oauth.OauthHelper;
import com.networknt.client.oauth.SignKeyRequest;
import com.networknt.client.oauth.TokenKeyRequest;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import com.networknt.exception.ClientException;
import com.networknt.exception.ExpiredTokenException;
import com.networknt.status.Status;
import com.networknt.utility.FingerPrintUtil;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.*;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.jose4j.keys.resolvers.X509VerificationKeyResolver;
import org.jose4j.lang.JoseException;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * This is a new class that is designed as non-static to replace the JwtHelper which is a static class. The reason
 * is to pass the framework specific security configuration so that we can eliminate the security.yml for token
 * verification.
 * <p>
 * The JwtHelper will be stayed for a while for backward compatibility reason as it is a public class and users might
 * use it in their application. The only thing that need to remember is to have both security.yml and openapi-security.yml
 * for the security configuration and there are overlap between these two files.
 * <p>
 * To use this class, create an instance by passing in the security configuration and cache the instance in your app
 * as a field or an instance variable.
 *
 * @author Steve Hu
 */
public class JwtVerifier extends TokenVerifier {
    static final Logger logger = LoggerFactory.getLogger(JwtVerifier.class);
    static final String GET_KEY_ERROR = "ERR10066";

    public static final String JWT = "jwt";
    public static final String JWK = "jwk";
    public static final String KID = "kid";
    public static final String SECURITY_CONFIG = "security";
    private static final int CACHE_EXPIRED_IN_MINUTES = 15;

    public static final String JWT_KEY_RESOLVER_X509CERT = "X509Certificate";
    public static final String JWT_KEY_RESOLVER_JWKS = "JsonWebKeySet";

    static SecurityConfig config;
    int secondsOfAllowedClockSkew;
    Boolean enableJwtCache;
    Boolean enableRelaxedKeyValidation;
    Boolean bootstrapFromKeyService;
    CacheManager cacheManager = CacheManager.getInstance();
    static Map<String, X509Certificate> certMap;
    static String audience;  // this is the audience from the client.yml with single oauth provider.
    static Map<String, String> audienceMap; // this is the audience map from the client.yml with multiple oauth providers.
    static List<String> fingerPrints;

    public JwtVerifier(SecurityConfig config) {
        this.config = config;
        this.secondsOfAllowedClockSkew = config.getClockSkewInSeconds();
        this.bootstrapFromKeyService = config.isBootstrapFromKeyService();
        this.enableRelaxedKeyValidation = config.isEnableRelaxedKeyValidation();
        this.enableJwtCache = config.isEnableJwtCache();
        // init getting JWK during the initialization. The other part is in the resolver for OAuth 2.0 provider to
        // rotate keys when the first token is received with the new kid.
        String keyResolver = config.getKeyResolver();
        // if KeyResolver is jwk and bootstrap from jwk is true, load jwk during server startup.
        if(logger.isTraceEnabled()) logger.trace("keyResolver = " + keyResolver + " bootstrapFromKeyService = " + bootstrapFromKeyService);
        if (JWT_KEY_RESOLVER_JWKS.equals(keyResolver) && bootstrapFromKeyService) {
            getJsonWebKeyMap();
        }
    }

    /**
     * This method is to keep backward compatible for those call without VerificationKeyResolver. The single
     * auth server is used in this case.
     *
     * @param jwt          JWT token
     * @param ignoreExpiry indicate if the expiry will be ignored
     * @param isToken      indicate if the JWT is a token
     * @param pathPrefix   pathPrefix used to cache the jwt token
     * @param requestPath  request path
     * @param jwkServiceIds A list of serviceIds from the UnifiedSecurityHandler
     * @return JwtClaims
     * @throws InvalidJwtException   throw when the token is invalid
     * @throws ExpiredTokenException throw when the token is expired
     */
    public JwtClaims verifyJwt(String jwt, boolean ignoreExpiry, boolean isToken, String pathPrefix, String requestPath, List<String> jwkServiceIds) throws InvalidJwtException, ExpiredTokenException {
        if(logger.isTraceEnabled()) logger.trace("verifyJwt is called with ignoreExpiry = " + ignoreExpiry + " isToken = " + isToken + " pathPrefix = " + pathPrefix + " requestPath = " + requestPath + " jwkServiceIds = " + jwkServiceIds);
        return verifyJwt(jwt, ignoreExpiry, isToken, pathPrefix, requestPath, jwkServiceIds, this::getKeyResolver);
    }

    /**
     * This method is to keep backward compatible for those call without VerificationKeyResolver. The single
     * auth server is used in this case.
     *
     * @param jwt          JWT token
     * @param ignoreExpiry indicate if the expiry will be ignored
     * @param isToken      indicate if the JWT is a token
     * @return JwtClaims
     * @throws InvalidJwtException   throw when the token is invalid
     * @throws ExpiredTokenException throw when the token is expired
     */
    public JwtClaims verifyJwt(String jwt, boolean ignoreExpiry, boolean isToken) throws InvalidJwtException, ExpiredTokenException {
        if(logger.isTraceEnabled()) logger.trace("verifyJwt is called with ignoreExpiry = " + ignoreExpiry + " isToken = " + isToken);
        return verifyJwt(jwt, ignoreExpiry, isToken, null, null, null, this::getKeyResolver);
    }

    /**
     * Verify JWT token format and signature. If ignoreExpiry is true, skip expiry verification, otherwise
     * verify the expiry before signature verification.
     * <p>
     * In most cases, we need to verify the expiry of the jwt token. The only time we need to ignore expiry
     * verification is in SPA middleware handlers which need to verify csrf token in jwt against the csrf
     * token in the request header to renew the expired token.
     *
     * @param jwt            String of Json web token
     * @param ignoreExpiry   If true, don't verify if the token is expired.
     * @param isToken        True if the jwt is an OAuth 2.0 access token
     * @param pathPrefix     pathPrefix for the jwt token cache key
     * @param getKeyResolver How to get VerificationKeyResolver
     * @param requestPath    the request path that used to find the right auth server config
     * @param jwkServiceIds  a list of jwk serviceIds defined in the client.yml to retrieve jwk.
     * @return JwtClaims object
     * @throws InvalidJwtException   InvalidJwtException
     * @throws ExpiredTokenException ExpiredTokenException
     */
    public JwtClaims verifyJwt(String jwt, boolean ignoreExpiry, boolean isToken, String pathPrefix, String requestPath, List<String> jwkServiceIds, BiFunction<String, Object, VerificationKeyResolver> getKeyResolver)
            throws InvalidJwtException, ExpiredTokenException {
        JwtClaims claims;
        String jwtJson = null;
        if (Boolean.TRUE.equals(enableJwtCache) && cacheManager != null) {
            if(pathPrefix != null) {
                jwtJson = (String)cacheManager.get(JWT, pathPrefix + ":" + jwt);
            } else {
                jwtJson = (String)cacheManager.get(JWT, jwt);
            }
            if (jwtJson != null) {
                try {
                    claims = JwtClaims.parse(jwtJson);
                } catch (InvalidJwtException e) {
                    logger.error("MalformedClaimException:", e);
                    throw new InvalidJwtException("MalformedClaimException", new ErrorCodeValidator.Error(ErrorCodes.MALFORMED_CLAIM, "Invalid JWT"), e, null);
                }
                checkExpiry(ignoreExpiry, claims, secondsOfAllowedClockSkew, null);
                // this claims object is signature verified already
                return claims;
            }
        }


        JwtConsumer consumer;
        JwtConsumerBuilder pKeyBuilder = new JwtConsumerBuilder()
                .setSkipAllValidators()
                .setDisableRequireSignature()
                .setSkipSignatureVerification();

        if (this.enableRelaxedKeyValidation) {
            pKeyBuilder.setRelaxVerificationKeyValidation();
        }

        consumer = pKeyBuilder.build();

        JwtContext jwtContext = consumer.process(jwt);
        claims = jwtContext.getJwtClaims();
        JsonWebStructure structure = jwtContext.getJoseObjects().get(0);
        // need this kid to load public key certificate for signature verification
        String kid = structure.getKeyIdHeaderValue();

        // so we do expiration check here manually as we have the claim already for kid
        // if ignoreExpiry is false, verify expiration of the token
        checkExpiry(ignoreExpiry, claims, secondsOfAllowedClockSkew, jwtContext);

        // validate the audience against the configured audience.
        validateAudience(claims, requestPath, jwkServiceIds, jwtContext);

        JwtConsumerBuilder jwtBuilder = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(315360000) // use seconds of 10 years to skip expiration validation as we need skip it in some cases.
                .setSkipDefaultAudienceValidation()
                .setVerificationKeyResolver(getKeyResolver.apply(kid, jwkServiceIds != null ? jwkServiceIds : requestPath));

        if (this.enableRelaxedKeyValidation) {
            jwtBuilder.setRelaxVerificationKeyValidation();
        }

        consumer = jwtBuilder.build();

        // Validate the JWT and process it to the Claims
        jwtContext = consumer.process(jwt);
        claims = jwtContext.getJwtClaims();
        if (Boolean.TRUE.equals(enableJwtCache) && cacheManager != null) {
            if(pathPrefix != null) {
                cacheManager.put(JWT, pathPrefix + ":" + jwt, claims.toJson());
            } else {
                cacheManager.put(JWT, jwt, claims.toJson());
            }
            if(cacheManager.getSize(JWT) > config.getJwtCacheFullSize()) {
                logger.warn("JWT cache exceeds the size limit " + config.getJwtCacheFullSize());
            }
        }
        return claims;
    }

    /**
     * validate the audience against the configured audience in the jwk section of the client.yml
     *
     * @param claims            - jwt claims
     * @param requestPath      - request path
     * @param jwkServiceIds    - a list of jwk service ids
     * @throws InvalidJwtException   - thrown when the token is malformed/invalid
     */
    private void validateAudience(JwtClaims claims, String requestPath, List<String> jwkServiceIds, JwtContext context) throws InvalidJwtException {
        ClientConfig clientConfig = ClientConfig.get();
        String configuredAudience = null;
        try {
            if (requestPath == null && jwkServiceIds == null) {
                // if both requestPath and jwkServiceIds are null, it means the single oauth server is used.
                configuredAudience = audience;
                if (configuredAudience != null) {
                    // validate the audience against the configured audience in the client.yml
                    boolean r = isJwtAudienceValid(claims, configuredAudience);
                    if (!r) {
                        throw new InvalidJwtException("Invalid Audience", Collections.singletonList(new ErrorCodeValidator.Error(ErrorCodes.AUDIENCE_INVALID, "Invalid Audience")), context);
                    }
                }
            } else if (jwkServiceIds != null && !jwkServiceIds.isEmpty()) {
                // more than one serviceIds are passed in from the UnifiedSecurityHandler. Just use each serviceId to get the audience.
                // this condition is in higher priority than the requestPath condition as requestPath will always be not null. The check
                // will iterate all the serviceIds and find the right audience. If anyone is matched, it will return true. None of them
                // is matched, it will return false.
                boolean r = isJwtAudienceValid(claims, jwkServiceIds);
                if(!r) {
                    throw new InvalidJwtException("Invalid Audience", Collections.singletonList(new ErrorCodeValidator.Error(ErrorCodes.AUDIENCE_INVALID, "Invalid Audience")), context);
                }
            } else {
                if (requestPath != null) {
                    String serviceId = getServiceIdByRequestPath(clientConfig, requestPath);
                    if(serviceId == null) {
                        // cannot find serviceId, get the single audience.
                        configuredAudience = audience;
                    } else {
                        // get the audience by serviceId from the audienceMap.
                        if(audienceMap != null && !audienceMap.isEmpty()) {
                            configuredAudience = audienceMap.get(serviceId);
                        }
                    }
                    if(configuredAudience != null) {
                        // validate the audience against the configured audience in the client.yml only if configuredAudience is not null.
                        boolean r = isJwtAudienceValid(claims, configuredAudience);
                        if (!r) {
                            throw new InvalidJwtException("Invalid Audience", Collections.singletonList(new ErrorCodeValidator.Error(ErrorCodes.AUDIENCE_INVALID, "Invalid Audience")), context);
                        }
                    }
                }
            }
        } catch (MalformedClaimException e) {
            logger.error("MalformedClaimException:", e);
            throw new InvalidJwtException("MalformedClaimException", new ErrorCodeValidator.Error(ErrorCodes.MALFORMED_CLAIM, "Invalid Audience"), e, context);
        }
    }

    private boolean isJwtAudienceValid(JwtClaims claims, String audience) throws MalformedClaimException {
        if (claims.getAudience() == null) {
            return false;
        }
        if (claims.getAudience().size() == 1) {
            return claims.getAudience().get(0).equals(audience);
        }
        return claims.getAudience().contains(audience);
    }

    private boolean isJwtAudienceValid(JwtClaims claims, List<String> jwkServiceIds) throws MalformedClaimException {
        // If audienceMap is null or empty, the audience validation is bypassed with true returned.
        if(audienceMap == null || audienceMap.isEmpty()) {
            return true;
        }
        // Iterate all the serviceIds and find the configured audience. If at least one of the serviceId has an audience configured, return the validation result.
        boolean validationResult = false; // the initial validation result is false.
        for(String serviceId: jwkServiceIds) {
            String configuredAudience = audienceMap.get(serviceId);
            if(configuredAudience == null || configuredAudience.isEmpty()) {
                // no audience configured for this serviceId, skip to the next one.
                continue;
            }
            validationResult = isJwtAudienceValid(claims, configuredAudience);
            if(validationResult) {
                break;
            }
        }
        return validationResult;
    }

    /**
     * Checks expiry of a jwt token from the claim.
     *
     * @param ignoreExpiry     - flag set if we want to ignore expired tokens or not.
     * @param claim            - jwt claims
     * @param allowedClockSkew - seconds of allowed skew in token expiry
     * @param context          - jwt context
     * @throws ExpiredTokenException - thrown when token is expired
     * @throws InvalidJwtException   - thrown when the token is malformed/invalid
     */
    private static void checkExpiry(boolean ignoreExpiry, JwtClaims claim, int allowedClockSkew, JwtContext context) throws ExpiredTokenException, InvalidJwtException {
        if (!ignoreExpiry) {
            try {
                // if using our own client module, the jwt token should be renewed automatically
                // and it will never expire here. However, we need to handle other clients.
                if ((NumericDate.now().getValue() - allowedClockSkew) >= claim.getExpirationTime().getValue()) {
                    logger.info("Cached jwt token is expired!");
                    throw new ExpiredTokenException("Token is expired");
                }
            } catch (MalformedClaimException e) {
                // This is cached token and it is impossible to have this exception
                logger.error("MalformedClaimException:", e);
                throw new InvalidJwtException("MalformedClaimException", new ErrorCodeValidator.Error(ErrorCodes.MALFORMED_CLAIM, "Invalid ExpirationTime Format"), e, context);
            }
        }
    }

    /**
     * Get VerificationKeyResolver based on the kid and isToken indicator. For the implementation, we check
     * the jwk first and 509Certificate if the jwk cannot find the kid. Basically, we want to iterate all
     * the resolvers and find the right one with the kid.
     *
     * @param kid         key id from the JWT token
     * @param requestPathOrJwkServiceIds the request path or jwkServiceIds of incoming request used to identify the serviceId to get the JWK.
     * @return VerificationKeyResolver
     */
    private VerificationKeyResolver getKeyResolver(String kid, Object requestPathOrJwkServiceIds) {
        // try the X509 certificate first
        String keyResolver = config.getKeyResolver();
        if(logger.isTraceEnabled()) logger.trace("kid = " + kid + " requestPathOrJwkServiceIds = " + requestPathOrJwkServiceIds + " keyResolver = " + keyResolver);
        // get the public key certificate from the cache that is loaded from security.yml. If it is not there,
        // go to the next step to access JWK if it is enabled. We need to update the light-oauth2 and oauth-kafka
        // to support JWK instead of X509Certificate endpoint. 
        X509Certificate certificate = certMap == null ? null : certMap.get(kid);
        if (certificate != null) {
            X509VerificationKeyResolver x509VerificationKeyResolver = new X509VerificationKeyResolver(certificate);
            x509VerificationKeyResolver.setTryAllOnNoThumbHeader(true);
            return x509VerificationKeyResolver;
        } else {
            if (JWT_KEY_RESOLVER_JWKS.equals(keyResolver)) {
                // try jwk if kid cannot be found in the certificate map.
                ClientConfig clientConfig = ClientConfig.get();
                List<JsonWebKey> jwkList = null;
                if(cacheManager != null) {
                    if(requestPathOrJwkServiceIds == null) {
                        // single oauth server, kid is the key for the jwk cache
                        jwkList = (List<JsonWebKey>)cacheManager.get(JWK, kid);
                    } else if(requestPathOrJwkServiceIds instanceof String) {
                        String requestPath = (String)requestPathOrJwkServiceIds;
                        // a single request path is passed in.
                        String serviceId = getServiceIdByRequestPath(clientConfig, requestPath);
                        if(serviceId == null) {
                            jwkList = (List<JsonWebKey>)cacheManager.get(JWK, kid);
                        } else {
                            jwkList = (List<JsonWebKey>)cacheManager.get(JWK, serviceId + ":" + kid);
                        }
                    } else if(requestPathOrJwkServiceIds instanceof List) {
                        List<String> serviceIds = (List)requestPathOrJwkServiceIds;
                        if(serviceIds != null && serviceIds.size() > 0) {
                            // more than one serviceIds are passed in from the UnifiedSecurityHandler. Just use the serviceId and kid
                            // combination to look up the jwkList. Once found, break the loop.
                            for(String serviceId: serviceIds) {
                                jwkList = (List<JsonWebKey>)cacheManager.get(JWK, serviceId + ":" + kid);
                                if(jwkList != null && jwkList.size() > 0) {
                                    break;
                                }
                            }
                        }
                    }
                }

                if (jwkList == null) {
                    jwkList = getJsonWebKeySetForToken(kid, requestPathOrJwkServiceIds);
                    if (jwkList == null || jwkList.isEmpty()) {
                        throw new RuntimeException("no JWK for kid: " + kid);
                    }
                    if(requestPathOrJwkServiceIds == null) {
                        // single jwk setup and kid is the key for the jwk cache.
                        cacheJwkList(jwkList, null);
                    } else if(requestPathOrJwkServiceIds instanceof String) {
                        // a single request path is passed in.
                        String serviceId = getServiceIdByRequestPath(clientConfig, (String)requestPathOrJwkServiceIds);
                        cacheJwkList(jwkList, serviceId);
                    } else if(requestPathOrJwkServiceIds instanceof List) {
                        // called with a list of serviceIds from the UnifiedSecurityHandler.
                        for(String serviceId: (List<String>)requestPathOrJwkServiceIds) {
                            cacheJwkList(jwkList, serviceId);
                        }
                    }
                }
                logger.debug("Got Json web key set from local cache");
                return new JwksVerificationKeyResolver(jwkList);
            } else {
                logger.error("Both X509Certificate and JWK are not configured.");
                return null;
            }
        }
    }

    private void cacheJwkList(List<JsonWebKey> jwkList, String serviceId) {
        if(cacheManager == null) return;
        for (JsonWebKey jwk : jwkList) {
            if(serviceId != null) {
                if(logger.isTraceEnabled()) logger.trace("cache the jwkList with serviceId {} kid {} and key {}", serviceId, jwk.getKeyId(), serviceId + ":" + jwk.getKeyId());
                cacheManager.put(JWK, serviceId + ":" + jwk.getKeyId(), jwkList);
            } else {
                if(logger.isTraceEnabled()) logger.trace("cache the jwkList with kid and only kid as key", jwk.getKeyId());
                cacheManager.put(JWK, jwk.getKeyId(), jwkList);
            }
        }
    }
    private String getServiceIdByRequestPath(ClientConfig clientConfig, String requestPath) {
        Map<String, String> pathPrefixServices = clientConfig.getPathPrefixServices();
        if(clientConfig.isMultipleAuthServers()) {
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
            return serviceId;
        } else {
            return null;
        }
    }

    /**
     * Retrieve JWK set from all possible oauth servers. If there are multiple servers in the client.yml, get all
     * the jwk by iterate all of them. In case we have multiple jwks, the cache will have a prefix so that verify
     * action won't cross fired.
     *
     */
    @SuppressWarnings("unchecked")
    private void getJsonWebKeyMap() {
        // the jwk indicator will ensure that the kid is not concat to the uri for path parameter.
        // the kid is not needed to get JWK. We need to figure out only one jwk server or multiple.
        ClientConfig clientConfig = ClientConfig.get();
        Map<String, Object> tokenConfig = clientConfig.getTokenConfig();
        Map<String, Object> keyConfig = (Map<String, Object>) tokenConfig.get(ClientConfig.KEY);
        if (clientConfig.isMultipleAuthServers()) {
            // iterate all the configured auth server to get JWK.
            Map<String, Object> serviceIdAuthServers = (Map<String, Object>) keyConfig.get(ClientConfig.SERVICE_ID_AUTH_SERVERS);
            if (serviceIdAuthServers != null && serviceIdAuthServers.size() > 0) {
                audienceMap = new HashMap<>();
                for (Map.Entry<String, Object> entry : serviceIdAuthServers.entrySet()) {
                    String serviceId = entry.getKey();
                    Map<String, Object> authServerConfig = (Map<String, Object>) entry.getValue();
                    // based on the configuration, we can identify if the entry is for jwk retrieval for jwt or swt introspection. For jwk,
                    // there is no clientId and clientSecret. For token introspection, clientId and clientSecret is in the config.
                    if(authServerConfig.get(ClientConfig.CLIENT_ID) != null && authServerConfig.get(ClientConfig.CLIENT_SECRET) != null) {
                        // this is the entry for swt introspection, skip here.
                        continue;
                    }
                    // construct audience map for audience validation.
                    String audience = (String) authServerConfig.get(ClientConfig.AUDIENCE);
                    if (audience != null) {
                        if (logger.isTraceEnabled()) logger.trace("audience {} is mapped to serviceId {}", audience, serviceId);
                        audienceMap.put(serviceId, audience);
                    }
                    // get the jwk from the auth server.
                    TokenKeyRequest keyRequest = new TokenKeyRequest(null, true, authServerConfig);
                    try {
                        if (logger.isDebugEnabled())
                            logger.debug("Getting Json Web Key list from {} for serviceId {}", keyRequest.getServerUrl(), entry.getKey());
                        String key = OauthHelper.getKey(keyRequest);
                        if (logger.isDebugEnabled())
                            logger.debug("Got Json Web Key = " + key);
                        List<JsonWebKey> jwkList = new JsonWebKeySet(key).getJsonWebKeys();
                        if (jwkList == null || jwkList.isEmpty()) {
                            if (logger.isErrorEnabled())
                                logger.error("Cannot get JWK from OAuth server.");
                        } else {
                            if(cacheManager != null) {
                                for (JsonWebKey jwk : jwkList) {
                                    cacheManager.put(JWK, serviceId + ":" + jwk.getKeyId(), jwkList);
                                    if (logger.isDebugEnabled())
                                        logger.debug("Successfully cached JWK for serviceId {} kid {} with key {}", serviceId, jwk.getKeyId(), serviceId + ":" + jwk.getKeyId());
                                }
                            }
                        }
                    } catch (JoseException ce) {
                        if (logger.isErrorEnabled())
                            logger.error("Failed to get JWK set. - {} - {}", new Status(GET_KEY_ERROR), ce.getMessage(), ce);

                    } catch (ClientException ce) {

                        if (logger.isErrorEnabled())
                            logger.error("Failed to get key. - {} - {} ", new Status(GET_KEY_ERROR), ce.getMessage(), ce);
                    }
                }
            } else {
                // log an error as there is no service entry for the jwk retrieval.
                logger.error("serviceIdAuthServers property is missing or empty in the token key configuration");
            }
        } else {
            // get audience from the key config
            audience = (String) keyConfig.get(ClientConfig.AUDIENCE);
            if(logger.isTraceEnabled()) logger.trace("A single audience {} is configured in client.yml", audience);
            // there is only one jwk server.
            TokenKeyRequest keyRequest = new TokenKeyRequest(null, true, null);
            try {
                if (logger.isDebugEnabled())
                    logger.debug("Getting Json Web Key list from {}", keyRequest.getServerUrl());

                String key = OauthHelper.getKey(keyRequest);

                if (logger.isDebugEnabled())
                    logger.debug("Got Json Web Key = " + key);

                List<JsonWebKey> jwkList = new JsonWebKeySet(key).getJsonWebKeys();
                if (jwkList == null || jwkList.isEmpty()) {
                    throw new RuntimeException("cannot get JWK from OAuth server");
                }
                if(cacheManager != null) {
                    for (JsonWebKey jwk : jwkList) {
                        cacheManager.put(JWK, jwk.getKeyId(), jwkList);

                        if (logger.isDebugEnabled())
                            logger.debug("Successfully cached JWK for kid {}", jwk.getKeyId());
                    }
                }
            } catch (JoseException ce) {

                if (logger.isErrorEnabled())
                    logger.error("Failed to get JWK. - {} - {}", new Status(GET_KEY_ERROR), ce.getMessage(), ce);

            } catch (ClientException ce) {

                if (logger.isErrorEnabled())
                    logger.error("Failed to get Key. - {} - {}", new Status(GET_KEY_ERROR), ce.getMessage(), ce);
            }
        }
    }

    /**
     * Retrieve JWK set from an oauth server with the kid. This method is used when a new kid is received
     * and the corresponding jwk doesn't exist in the cache. It will look up the key service by kid first.
     *
     * @param kid         String of kid
     * @param requestPathOrJwkServiceIds String of request path or list of strings for jwkServiceIds
     * @return {@link List} of {@link JsonWebKey}
     */
    @SuppressWarnings("unchecked")
    private List<JsonWebKey> getJsonWebKeySetForToken(String kid, Object requestPathOrJwkServiceIds) {
        // the jwk indicator will ensure that the kid is not concat to the uri for path parameter.
        // the kid is not needed to get JWK, but if requestPath is not null, it will be used to get the keyConfig
        if (logger.isTraceEnabled()) {
            logger.trace("kid = " + kid + " requestPathOrJwkServiceIds = " + requestPathOrJwkServiceIds);
            if(requestPathOrJwkServiceIds instanceof List) {
                ((List<String>)requestPathOrJwkServiceIds).forEach(logger::trace);
            }
        }
        ClientConfig clientConfig = ClientConfig.get();
        List<JsonWebKey> jwks = null;
        Map<String, Object> config = null;

        if (requestPathOrJwkServiceIds != null && clientConfig.isMultipleAuthServers()) {
            if(requestPathOrJwkServiceIds instanceof String) {
                String requestPath = (String)requestPathOrJwkServiceIds;
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
                jwks = retrieveJwk(kid, config);
            } else if (requestPathOrJwkServiceIds instanceof List) {
                List<String> jwkServiceIds = (List<String>)requestPathOrJwkServiceIds;
                jwks = new ArrayList<>();
                for(String serviceId: jwkServiceIds) {
                    config = getJwkConfig(clientConfig, serviceId);
                    jwks.addAll(retrieveJwk(kid, config));
                }
            } else {
                throw new ConfigException("requestPathOrJwkServiceIds must be a string or a list of strings");
            }
        } else {
            // get the jwk from the key section in the client.yml token key.
            jwks = retrieveJwk(kid, null);
        }
        return jwks;
    }


    private List<JsonWebKey> retrieveJwk(String kid, Map<String, Object> config) {
        // get the jwk with the kid and config map.
        if (logger.isTraceEnabled() && config != null)
            logger.trace("multiple oauth config based on path = " + JsonMapper.toJson(config));
        // config is not null if isMultipleAuthServers is true. If it is null, then the key section is used from the client.yml
        TokenKeyRequest keyRequest = new TokenKeyRequest(kid, true, config);

        try {
            if (logger.isDebugEnabled())
                logger.debug("Getting Json Web Key list from {}", keyRequest.getServerUrl());

            String key = OauthHelper.getKey(keyRequest);

            if (logger.isDebugEnabled())
                logger.debug("Got Json Web Key {} from {} with path {}", key, keyRequest.getServerUrl(), keyRequest.getUri());

            return new JsonWebKeySet(key).getJsonWebKeys();
        } catch (JoseException ce) {
            if (logger.isErrorEnabled())
                logger.error("Failed to get JWK. - {} - {}", new Status(GET_KEY_ERROR), ce.getMessage(), ce);
        } catch (ClientException ce) {
            if (logger.isErrorEnabled())
                logger.error("Failed to get key - {} - {}", new Status(GET_KEY_ERROR), ce.getMessage(), ce);
        }
        return null;
    }

    /**
     * Get cert for specified kid.
     *
     * @param kid - kid used to lookup cert.
     * @return - x509 cert associated with kid.
     */
    public X509Certificate getCertForToken(String kid) {
        X509Certificate certificate = null;
        TokenKeyRequest keyRequest = new TokenKeyRequest(kid);
        try {

            if (logger.isWarnEnabled())
                logger.warn("<Deprecated: use JsonWebKeySet instead> Getting raw certificate for kid: {} from {}", kid, keyRequest.getServerUrl());

            String key = OauthHelper.getKey(keyRequest);

            if (logger.isWarnEnabled())
                logger.warn("<Deprecated: use JsonWebKeySet instead> Got raw certificate {} for kid: {}", key, kid);

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(key.getBytes(StandardCharsets.UTF_8)));
        } catch (CertificateException ce) {

            if (logger.isErrorEnabled())
                logger.error("Failed to generate certificate: {}", ce.getMessage(), ce);

        } catch (ClientException ce) {

            if (logger.isErrorEnabled())
                logger.error("Failed to get key: {}", ce.getMessage(), ce);

        }
        return certificate;
    }

    public X509Certificate getCertForSign(String kid) {
        X509Certificate certificate = null;
        SignKeyRequest keyRequest = new SignKeyRequest(kid);
        try {
            String key = OauthHelper.getKey(keyRequest);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(key.getBytes(StandardCharsets.UTF_8)));
        } catch (CertificateException ce) {

            if (logger.isErrorEnabled())
                logger.error("Failed to generate certificate: {}", ce.getMessage(), ce);

        } catch (ClientException ce) {

            if (logger.isErrorEnabled())
                logger.error("Failed to get key: {}", ce.getMessage(), ce);

        }
        return certificate;
    }

    /**
     * Get a list of certificate fingerprints for server info endpoint so that certification process in light-portal
     * can detect if your service still use the default public key certificates provided by the light-4j framework.
     * <p>
     * The default public key certificates are for dev only and should be replaced on any other environment or
     * set bootstrapFromKeyService: true if you are using light-oauth2 so that key can be dynamically loaded.
     *
     * @return List of certificate fingerprints
     */
    public List<String> getFingerPrints() {
        return fingerPrints;
    }

}
