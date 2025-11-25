package com.networknt.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.schema.IntegerField;
import com.networknt.config.schema.generator.DebugGenerator;
import com.networknt.config.schema.generator.JsonSchemaGenerator;
import com.networknt.config.schema.generator.YamlGenerator;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;

import static org.junit.Assert.*;

public class JsonSchemaGeneratorTest {
public static final String clientConfigMetadata = "{\n" +
        "  \"properties\" : {\n" +
        "    \"tls\" : {\n" +
        "      \"type\" : \"object\",\n" +
        "      \"$id\" : \"c802749e-7100-42db-9e88-ff5e0cdbd928\",\n" +
        "      \"externalizedKeyName\" : \"\",\n" +
        "      \"configFieldName\" : \"tls\",\n" +
        "      \"description\" : \"Settings for TLS\",\n" +
        "      \"externalized\" : false,\n" +
        "      \"useSubObjectDefault\" : true,\n" +
        "      \"defaultValue\" : \"\",\n" +
        "      \"ref\" : {\n" +
        "        \"properties\" : {\n" +
        "          \"verifyHostname\" : {\n" +
        "            \"type\" : \"boolean\",\n" +
        "            \"$id\" : \"b1ebffc1-48e8-43de-9ebf-75d1c04a1917\",\n" +
        "            \"externalizedKeyName\" : \"verifyHostname\",\n" +
        "            \"configFieldName\" : \"verifyHostname\",\n" +
        "            \"description\" : \"if the server is using self-signed certificate, this need to be false. If true, you have to use CA signed certificate or load\\ntruststore that contains the self-signed certificate.\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : true\n" +
        "          },\n" +
        "          \"loadDefaultTrustStore\" : {\n" +
        "            \"type\" : \"boolean\",\n" +
        "            \"$id\" : \"5b1f9a2e-d754-44e6-a04e-222d2d818e79\",\n" +
        "            \"externalizedKeyName\" : \"loadDefaultTrustStore\",\n" +
        "            \"configFieldName\" : \"loadDefaultTrustStore\",\n" +
        "            \"description\" : \"indicate of system load default cert.\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : true\n" +
        "          },\n" +
        "          \"loadTrustStore\" : {\n" +
        "            \"type\" : \"boolean\",\n" +
        "            \"$id\" : \"4e601164-d55d-43db-91ca-0ec26cab7542\",\n" +
        "            \"externalizedKeyName\" : \"loadTrustStore\",\n" +
        "            \"configFieldName\" : \"loadTrustStore\",\n" +
        "            \"description\" : \"trust store contains certificates that server needs. Enable if tls is used.\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : true\n" +
        "          },\n" +
        "          \"trustStore\" : {\n" +
        "            \"type\" : \"string\",\n" +
        "            \"$id\" : \"fc98d7cd-de2e-46c5-92b1-239a18a4fc6a\",\n" +
        "            \"externalizedKeyName\" : \"trustStore\",\n" +
        "            \"configFieldName\" : \"trustStore\",\n" +
        "            \"description\" : \"trust store location can be specified here or system properties javax.net.ssl.trustStore and password javax.net.ssl.trustStorePassword\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : \"client.truststore\",\n" +
        "            \"minLength\" : 0,\n" +
        "            \"maxLength\" : 2147483647,\n" +
        "            \"pattern\" : \"\",\n" +
        "            \"format\" : \"none\"\n" +
        "          },\n" +
        "          \"trustStorePass\" : {\n" +
        "            \"type\" : \"string\",\n" +
        "            \"$id\" : \"8d35e32e-40f2-4030-8da1-cdacc2f31afd\",\n" +
        "            \"externalizedKeyName\" : \"trustStorePass\",\n" +
        "            \"configFieldName\" : \"trustStorePass\",\n" +
        "            \"description\" : \"trust store password\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : \"password\",\n" +
        "            \"minLength\" : 0,\n" +
        "            \"maxLength\" : 2147483647,\n" +
        "            \"pattern\" : \"\",\n" +
        "            \"format\" : \"none\"\n" +
        "          },\n" +
        "          \"loadKeyStore\" : {\n" +
        "            \"type\" : \"boolean\",\n" +
        "            \"$id\" : \"61d04304-5fd7-4359-9886-2f64b5b51db6\",\n" +
        "            \"externalizedKeyName\" : \"loadKeyStore\",\n" +
        "            \"configFieldName\" : \"loadKeyStore\",\n" +
        "            \"description\" : \"key store contains client key and it should be loaded if two-way ssl is used.\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : false\n" +
        "          },\n" +
        "          \"keyStore\" : {\n" +
        "            \"type\" : \"string\",\n" +
        "            \"$id\" : \"4733f098-8565-451d-a2e1-7d82eb2b0b16\",\n" +
        "            \"externalizedKeyName\" : \"keyStore\",\n" +
        "            \"configFieldName\" : \"keyStore\",\n" +
        "            \"description\" : \"key store location\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : \"client.keystore\",\n" +
        "            \"minLength\" : 0,\n" +
        "            \"maxLength\" : 2147483647,\n" +
        "            \"pattern\" : \"\",\n" +
        "            \"format\" : \"none\"\n" +
        "          },\n" +
        "          \"keyStorePass\" : {\n" +
        "            \"type\" : \"string\",\n" +
        "            \"$id\" : \"de3ac4d2-736e-498c-ac5f-686782f2e359\",\n" +
        "            \"externalizedKeyName\" : \"keyStorePass\",\n" +
        "            \"configFieldName\" : \"keyStorePass\",\n" +
        "            \"description\" : \"key store password\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : \"password\",\n" +
        "            \"minLength\" : 0,\n" +
        "            \"maxLength\" : 2147483647,\n" +
        "            \"pattern\" : \"\",\n" +
        "            \"format\" : \"none\"\n" +
        "          },\n" +
        "          \"keyPass\" : {\n" +
        "            \"type\" : \"string\",\n" +
        "            \"$id\" : \"cc261c08-7f94-4676-9236-9dd0289c2917\",\n" +
        "            \"externalizedKeyName\" : \"keyPass\",\n" +
        "            \"configFieldName\" : \"keyPass\",\n" +
        "            \"description\" : \"private key password\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : \"password\",\n" +
        "            \"minLength\" : 0,\n" +
        "            \"maxLength\" : 2147483647,\n" +
        "            \"pattern\" : \"\",\n" +
        "            \"format\" : \"none\"\n" +
        "          },\n" +
        "          \"defaultCertPassword\" : {\n" +
        "            \"type\" : \"string\",\n" +
        "            \"$id\" : \"2a2ab2f5-da86-40fd-940a-baf67760389b\",\n" +
        "            \"externalizedKeyName\" : \"defaultCertPassword\",\n" +
        "            \"configFieldName\" : \"defaultCertPassword\",\n" +
        "            \"description\" : \"public issued CA cert password\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : \"chageit\",\n" +
        "            \"minLength\" : 0,\n" +
        "            \"maxLength\" : 2147483647,\n" +
        "            \"pattern\" : \"\",\n" +
        "            \"format\" : \"none\"\n" +
        "          },\n" +
        "          \"tlsVersion\" : {\n" +
        "            \"type\" : \"string\",\n" +
        "            \"$id\" : \"63318aca-288e-4cbe-bad4-bda1a375cf55\",\n" +
        "            \"externalizedKeyName\" : \"tlsVersion\",\n" +
        "            \"configFieldName\" : \"tlsVersion\",\n" +
        "            \"description\" : \"TLS version. Default is TSLv1.3, and you can downgrade to TLSv1.2 to support some internal old servers that support only TLSv1.1\\nand 1.2 (deprecated and risky).\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : \"TLSv1.3\",\n" +
        "            \"minLength\" : 0,\n" +
        "            \"maxLength\" : 2147483647,\n" +
        "            \"pattern\" : \"\",\n" +
        "            \"format\" : \"none\"\n" +
        "          }\n" +
        "        },\n" +
        "        \"type\" : \"object\"\n" +
        "      }\n" +
        "    },\n" +
        "    \"oauth\" : {\n" +
        "      \"type\" : \"object\",\n" +
        "      \"$id\" : \"b9589680-5dcc-4bbc-9cfe-0221479a9514\",\n" +
        "      \"externalizedKeyName\" : \"\",\n" +
        "      \"configFieldName\" : \"oauth\",\n" +
        "      \"description\" : \"Settings for OAuth2 server communication.\",\n" +
        "      \"externalized\" : false,\n" +
        "      \"useSubObjectDefault\" : true,\n" +
        "      \"defaultValue\" : \"\",\n" +
        "      \"ref\" : {\n" +
        "        \"properties\" : {\n" +
        "          \"multipleAuthServers\" : {\n" +
        "            \"type\" : \"boolean\",\n" +
        "            \"$id\" : \"87a6b0aa-bada-4610-a96d-25e83b931123\",\n" +
        "            \"externalizedKeyName\" : \"multipleAuthServers\",\n" +
        "            \"configFieldName\" : \"multipleAuthServers\",\n" +
        "            \"description\" : \"OAuth 2.0 token endpoint configuration\\nIf there are multiple oauth providers per serviceId, then we need to update this flag to true. In order to derive the serviceId from the\\npath prefix, we need to set up the pathPrefixServices below if there is no duplicated paths between services.\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : false\n" +
        "          },\n" +
        "          \"token\" : {\n" +
        "            \"type\" : \"object\",\n" +
        "            \"$id\" : \"9d1bae78-7596-4463-bbdb-6e71f761bf6a\",\n" +
        "            \"externalizedKeyName\" : \"\",\n" +
        "            \"configFieldName\" : \"token\",\n" +
        "            \"description\" : \"\",\n" +
        "            \"externalized\" : false,\n" +
        "            \"useSubObjectDefault\" : true,\n" +
        "            \"defaultValue\" : \"\",\n" +
        "            \"ref\" : {\n" +
        "              \"properties\" : {\n" +
        "                \"cache\" : {\n" +
        "                  \"type\" : \"object\",\n" +
        "                  \"$id\" : \"e533feef-0f1f-4ec5-a341-ef81d9694b6e\",\n" +
        "                  \"externalizedKeyName\" : \"\",\n" +
        "                  \"configFieldName\" : \"cache\",\n" +
        "                  \"description\" : \"\",\n" +
        "                  \"externalized\" : false,\n" +
        "                  \"useSubObjectDefault\" : true,\n" +
        "                  \"defaultValue\" : \"\",\n" +
        "                  \"ref\" : {\n" +
        "                    \"properties\" : {\n" +
        "                      \"capacity\" : {\n" +
        "                        \"type\" : \"integer\",\n" +
        "                        \"$id\" : \"90bce76a-26e6-4f30-9d87-0d5a90aae65e\",\n" +
        "                        \"externalizedKeyName\" : \"tokenCacheCapacity\",\n" +
        "                        \"configFieldName\" : \"capacity\",\n" +
        "                        \"description\" : \"capacity of caching tokens in the client for downstream API calls.\",\n" +
        "                        \"externalized\" : false,\n" +
        "                        \"defaultValue\" : 200,\n" +
        "                        \"minimum\" : -2147483648,\n" +
        "                        \"maximum\" : 2147483647,\n" +
        "                        \"exclusiveMin\" : false,\n" +
        "                        \"exclusiveMax\" : false,\n" +
        "                        \"multipleOf\" : 0,\n" +
        "                        \"format\" : \"int32\"\n" +
        "                      }\n" +
        "                    },\n" +
        "                    \"type\" : \"object\"\n" +
        "                  }\n" +
        "                },\n" +
        "                \"tokenRenewBeforeExpired\" : {\n" +
        "                  \"type\" : \"integer\",\n" +
        "                  \"$id\" : \"39321a0f-4b52-4e66-bfea-b483d5eaca72\",\n" +
        "                  \"externalizedKeyName\" : \"tokenRenewBeforeExpired\",\n" +
        "                  \"configFieldName\" : \"tokenRenewBeforeExpired\",\n" +
        "                  \"description\" : \"The scope token will be renewed automatically 1 minute before expiry\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : 60000,\n" +
        "                  \"minimum\" : -2147483648,\n" +
        "                  \"maximum\" : 2147483647,\n" +
        "                  \"exclusiveMin\" : false,\n" +
        "                  \"exclusiveMax\" : false,\n" +
        "                  \"multipleOf\" : 0,\n" +
        "                  \"format\" : \"int32\"\n" +
        "                },\n" +
        "                \"expiredRefreshRetryDelay\" : {\n" +
        "                  \"type\" : \"integer\",\n" +
        "                  \"$id\" : \"e0a9885e-3325-4760-95f4-17823c82f30b\",\n" +
        "                  \"externalizedKeyName\" : \"expiredRefreshRetryDelay\",\n" +
        "                  \"configFieldName\" : \"expiredRefreshRetryDelay\",\n" +
        "                  \"description\" : \"if scope token is expired, we need short delay so that we can retry faster.\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : 2000,\n" +
        "                  \"minimum\" : -2147483648,\n" +
        "                  \"maximum\" : 2147483647,\n" +
        "                  \"exclusiveMin\" : false,\n" +
        "                  \"exclusiveMax\" : false,\n" +
        "                  \"multipleOf\" : 0,\n" +
        "                  \"format\" : \"int32\"\n" +
        "                },\n" +
        "                \"earlyRefreshRetryDelay\" : {\n" +
        "                  \"type\" : \"integer\",\n" +
        "                  \"$id\" : \"5dfeb1a0-446e-4983-80a4-8b84883bd75a\",\n" +
        "                  \"externalizedKeyName\" : \"earlyRefreshRetryDelay\",\n" +
        "                  \"configFieldName\" : \"earlyRefreshRetryDelay\",\n" +
        "                  \"description\" : \"if scope token is not expired but in renew window, we need slow retry delay.\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : 4000,\n" +
        "                  \"minimum\" : -2147483648,\n" +
        "                  \"maximum\" : 2147483647,\n" +
        "                  \"exclusiveMin\" : false,\n" +
        "                  \"exclusiveMax\" : false,\n" +
        "                  \"multipleOf\" : 0,\n" +
        "                  \"format\" : \"int32\"\n" +
        "                },\n" +
        "                \"server_url\" : {\n" +
        "                  \"type\" : \"string\",\n" +
        "                  \"$id\" : \"e9bf4d2a-fa44-48d3-98a2-a15314a02408\",\n" +
        "                  \"externalizedKeyName\" : \"tokenServerUrl\",\n" +
        "                  \"configFieldName\" : \"server_url\",\n" +
        "                  \"description\" : \"token server url. The default port number for token service is 6882. If this is set,\\nit will take high priority than serviceId for the direct connection\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : \"\",\n" +
        "                  \"minLength\" : 0,\n" +
        "                  \"maxLength\" : 2147483647,\n" +
        "                  \"pattern\" : \"\",\n" +
        "                  \"format\" : \"none\"\n" +
        "                },\n" +
        "                \"serviceId\" : {\n" +
        "                  \"type\" : \"string\",\n" +
        "                  \"$id\" : \"ab0a9508-5169-4eb9-8c22-66e695c2bf22\",\n" +
        "                  \"externalizedKeyName\" : \"tokenServiceId\",\n" +
        "                  \"configFieldName\" : \"serviceId\",\n" +
        "                  \"description\" : \"token service unique id for OAuth 2.0 provider. If server_url is not set above,\\na service discovery action will be taken to find an instance of token service.\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : \"com.networknt.oauth2-token-1.0.0\",\n" +
        "                  \"minLength\" : 0,\n" +
        "                  \"maxLength\" : 2147483647,\n" +
        "                  \"pattern\" : \"\",\n" +
        "                  \"format\" : \"none\"\n" +
        "                },\n" +
        "                \"proxyHost\" : {\n" +
        "                  \"type\" : \"string\",\n" +
        "                  \"$id\" : \"4fcf6681-ae10-4507-bc95-642fd154dc59\",\n" +
        "                  \"externalizedKeyName\" : \"tokenProxyHost\",\n" +
        "                  \"configFieldName\" : \"proxyHost\",\n" +
        "                  \"description\" : \"For users who leverage SaaS OAuth 2.0 provider from lightapi.net or others in the public cloud\\nand has an internal proxy server to access code, token and key services of OAuth 2.0, set up the\\nproxyHost here for the HTTPS traffic. This option is only working with server_url and serviceId\\nbelow should be commented out. OAuth 2.0 services cannot be discovered if a proxy server is used.\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : \"\",\n" +
        "                  \"minLength\" : 0,\n" +
        "                  \"maxLength\" : 2147483647,\n" +
        "                  \"pattern\" : \"\",\n" +
        "                  \"format\" : \"none\"\n" +
        "                },\n" +
        "                \"proxyPort\" : {\n" +
        "                  \"type\" : \"integer\",\n" +
        "                  \"$id\" : \"dbe2aeae-285e-4369-8e24-fd2094de8594\",\n" +
        "                  \"externalizedKeyName\" : \"tokenProxyPort\",\n" +
        "                  \"configFieldName\" : \"proxyPort\",\n" +
        "                  \"description\" : \"We only support HTTPS traffic for the proxy and the default port is 443. If your proxy server has\\na different port, please specify it here. If proxyHost is available and proxyPort is missing, then\\nthe default value 443 is going to be used for the HTTP connection.\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : 0,\n" +
        "                  \"minimum\" : -2147483648,\n" +
        "                  \"maximum\" : 2147483647,\n" +
        "                  \"exclusiveMin\" : false,\n" +
        "                  \"exclusiveMax\" : false,\n" +
        "                  \"multipleOf\" : 0,\n" +
        "                  \"format\" : \"int32\"\n" +
        "                },\n" +
        "                \"enableHttp2\" : {\n" +
        "                  \"type\" : \"boolean\",\n" +
        "                  \"$id\" : \"9b3c6df5-e0d3-426a-afd3-42be45c46a32\",\n" +
        "                  \"externalizedKeyName\" : \"tokenEnableHttp2\",\n" +
        "                  \"configFieldName\" : \"enableHttp2\",\n" +
        "                  \"description\" : \"set to true if the oauth2 provider supports HTTP/2\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : true\n" +
        "                },\n" +
        "                \"authorization_code\" : {\n" +
        "                  \"type\" : \"object\",\n" +
        "                  \"$id\" : \"a68b4436-554c-40cd-9145-7f2770d0a1fc\",\n" +
        "                  \"externalizedKeyName\" : \"\",\n" +
        "                  \"configFieldName\" : \"authorization_code\",\n" +
        "                  \"description\" : \"the following section defines uri and parameters for authorization code grant type\",\n" +
        "                  \"externalized\" : false,\n" +
        "                  \"useSubObjectDefault\" : true,\n" +
        "                  \"defaultValue\" : \"\",\n" +
        "                  \"ref\" : {\n" +
        "                    \"properties\" : {\n" +
        "                      \"uri\" : {\n" +
        "                        \"type\" : \"string\",\n" +
        "                        \"$id\" : \"68bff9d6-69b4-42f5-96ea-78399c74861a\",\n" +
        "                        \"externalizedKeyName\" : \"tokenAcUri\",\n" +
        "                        \"configFieldName\" : \"uri\",\n" +
        "                        \"description\" : \"token endpoint for authorization code grant\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"defaultValue\" : \"/oauth2/token\",\n" +
        "                        \"minLength\" : 0,\n" +
        "                        \"maxLength\" : 2147483647,\n" +
        "                        \"pattern\" : \"\",\n" +
        "                        \"format\" : \"none\"\n" +
        "                      },\n" +
        "                      \"client_id\" : {\n" +
        "                        \"type\" : \"string\",\n" +
        "                        \"$id\" : \"1e511c42-e0d5-4180-80f2-533b67b7b091\",\n" +
        "                        \"externalizedKeyName\" : \"tokenAcClientId\",\n" +
        "                        \"configFieldName\" : \"client_id\",\n" +
        "                        \"description\" : \"client_id for authorization code grant flow.\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"defaultValue\" : \"f7d42348-c647-4efb-a52d-4c5787421e72\",\n" +
        "                        \"minLength\" : 0,\n" +
        "                        \"maxLength\" : 2147483647,\n" +
        "                        \"pattern\" : \"\",\n" +
        "                        \"format\" : \"none\"\n" +
        "                      },\n" +
        "                      \"client_secret\" : {\n" +
        "                        \"type\" : \"string\",\n" +
        "                        \"$id\" : \"bbdb5243-da92-4253-a975-a9569c2d6197\",\n" +
        "                        \"externalizedKeyName\" : \"tokenAcClientSecret\",\n" +
        "                        \"configFieldName\" : \"client_secret\",\n" +
        "                        \"description\" : \"client_secret for authorization code grant flow.\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"defaultValue\" : \"f6h1FTI8Q3-7UScPZDzfXA\",\n" +
        "                        \"minLength\" : 0,\n" +
        "                        \"maxLength\" : 2147483647,\n" +
        "                        \"pattern\" : \"\",\n" +
        "                        \"format\" : \"none\"\n" +
        "                      },\n" +
        "                      \"redirect_uri\" : {\n" +
        "                        \"type\" : \"string\",\n" +
        "                        \"$id\" : \"14f20328-dd6a-457d-9777-244f938b2299\",\n" +
        "                        \"externalizedKeyName\" : \"tokenAcRedirectUri\",\n" +
        "                        \"configFieldName\" : \"redirect_uri\",\n" +
        "                        \"description\" : \"the web server uri that will receive the redirected authorization code\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"defaultValue\" : \"http://localhost:8080/authorization\",\n" +
        "                        \"minLength\" : 0,\n" +
        "                        \"maxLength\" : 2147483647,\n" +
        "                        \"pattern\" : \"\",\n" +
        "                        \"format\" : \"none\"\n" +
        "                      },\n" +
        "                      \"scope\" : {\n" +
        "                        \"type\" : \"array\",\n" +
        "                        \"$id\" : \"82c7163d-36ca-47d9-8e11-99d5f5eda467\",\n" +
        "                        \"externalizedKeyName\" : \"tokenAcScope\",\n" +
        "                        \"configFieldName\" : \"scope\",\n" +
        "                        \"description\" : \"optional scope, default scope in the client registration will be used if not defined.\\nIf there are scopes specified here, they will be verified against the registered scopes.\\nIn values.yml, you define a list of strings for the scope(s).\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"items\" : {\n" +
        "                          \"type\" : \"string\"\n" +
        "                        },\n" +
        "                        \"minItems\" : 0,\n" +
        "                        \"maxItems\" : 2147483647,\n" +
        "                        \"uniqueItems\" : false,\n" +
        "                        \"contains\" : false,\n" +
        "                        \"useSubObjectDefault\" : false,\n" +
        "                        \"defaultValue\" : \"\"\n" +
        "                      }\n" +
        "                    },\n" +
        "                    \"type\" : \"object\"\n" +
        "                  }\n" +
        "                },\n" +
        "                \"client_credentials\" : {\n" +
        "                  \"type\" : \"object\",\n" +
        "                  \"$id\" : \"577ecfd9-abf3-4031-b079-5c02fd7f38b3\",\n" +
        "                  \"externalizedKeyName\" : \"\",\n" +
        "                  \"configFieldName\" : \"client_credentials\",\n" +
        "                  \"description\" : \"the following section defines uri and parameters for client credentials grant type\",\n" +
        "                  \"externalized\" : false,\n" +
        "                  \"useSubObjectDefault\" : true,\n" +
        "                  \"defaultValue\" : \"\",\n" +
        "                  \"ref\" : {\n" +
        "                    \"properties\" : {\n" +
        "                      \"uri\" : {\n" +
        "                        \"type\" : \"string\",\n" +
        "                        \"$id\" : \"b4a82515-2222-4ef1-86ea-6d7a8b14f16f\",\n" +
        "                        \"externalizedKeyName\" : \"tokenCcUri\",\n" +
        "                        \"configFieldName\" : \"uri\",\n" +
        "                        \"description\" : \"token endpoint for client credentials grant\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"defaultValue\" : \"/oauth2/token\",\n" +
        "                        \"minLength\" : 0,\n" +
        "                        \"maxLength\" : 2147483647,\n" +
        "                        \"pattern\" : \"\",\n" +
        "                        \"format\" : \"none\"\n" +
        "                      },\n" +
        "                      \"client_id\" : {\n" +
        "                        \"type\" : \"string\",\n" +
        "                        \"$id\" : \"64c220a5-7ec2-4187-bf77-5f626a986224\",\n" +
        "                        \"externalizedKeyName\" : \"tokenCcClientId\",\n" +
        "                        \"configFieldName\" : \"client_id\",\n" +
        "                        \"description\" : \"client_id for client credentials grant flow.\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"defaultValue\" : \"f7d42348-c647-4efb-a52d-4c5787421e72\",\n" +
        "                        \"minLength\" : 0,\n" +
        "                        \"maxLength\" : 2147483647,\n" +
        "                        \"pattern\" : \"\",\n" +
        "                        \"format\" : \"none\"\n" +
        "                      },\n" +
        "                      \"client_secret\" : {\n" +
        "                        \"type\" : \"string\",\n" +
        "                        \"$id\" : \"b0985c82-0c2e-4cb0-a72c-f6c71fd29cc0\",\n" +
        "                        \"externalizedKeyName\" : \"tokenCcClientSecret\",\n" +
        "                        \"configFieldName\" : \"client_secret\",\n" +
        "                        \"description\" : \"client_secret for client credentials grant flow.\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"defaultValue\" : \"f6h1FTI8Q3-7UScPZDzfXA\",\n" +
        "                        \"minLength\" : 0,\n" +
        "                        \"maxLength\" : 2147483647,\n" +
        "                        \"pattern\" : \"\",\n" +
        "                        \"format\" : \"none\"\n" +
        "                      },\n" +
        "                      \"scope\" : {\n" +
        "                        \"type\" : \"array\",\n" +
        "                        \"$id\" : \"fe1c0720-b6b8-485a-8429-620e039917ce\",\n" +
        "                        \"externalizedKeyName\" : \"tokenCcScope\",\n" +
        "                        \"configFieldName\" : \"scope\",\n" +
        "                        \"description\" : \"optional scope, default scope in the client registration will be used if not defined.\\nIf there are scopes specified here, they will be verified against the registered scopes.\\nIn values.yml, you define a list of strings for the scope(s).\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"items\" : {\n" +
        "                          \"type\" : \"string\"\n" +
        "                        },\n" +
        "                        \"minItems\" : 0,\n" +
        "                        \"maxItems\" : 2147483647,\n" +
        "                        \"uniqueItems\" : false,\n" +
        "                        \"contains\" : false,\n" +
        "                        \"useSubObjectDefault\" : false,\n" +
        "                        \"defaultValue\" : \"\"\n" +
        "                      },\n" +
        "                      \"serviceIdAuthServers\" : {\n" +
        "                        \"type\" : \"map\",\n" +
        "                        \"$id\" : \"a4fd0e34-7467-4d67-91ab-5989c5e3f7bf\",\n" +
        "                        \"externalizedKeyName\" : \"tokenCcServiceIdAuthServers\",\n" +
        "                        \"configFieldName\" : \"serviceIdAuthServers\",\n" +
        "                        \"description\" : \"The serviceId to the service specific OAuth 2.0 configuration. Used only when multipleOAuthServer is\\nset as true. For detailed config options, please see the values.yml in the client module test.\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"additionalProperties\" : {\n" +
        "                          \"properties\" : {\n" +
        "                            \"server_url\" : {\n" +
        "                              \"type\" : \"string\",\n" +
        "                              \"$id\" : \"f31b2033-b229-4772-894c-f4c5992fe9dc\",\n" +
        "                              \"externalizedKeyName\" : \"\",\n" +
        "                              \"configFieldName\" : \"server_url\",\n" +
        "                              \"description\" : \"\",\n" +
        "                              \"externalized\" : false,\n" +
        "                              \"defaultValue\" : \"\",\n" +
        "                              \"minLength\" : 0,\n" +
        "                              \"maxLength\" : 2147483647,\n" +
        "                              \"pattern\" : \"\",\n" +
        "                              \"format\" : \"none\"\n" +
        "                            },\n" +
        "                            \"enableHttp2\" : {\n" +
        "                              \"type\" : \"boolean\",\n" +
        "                              \"$id\" : \"d04a3b6e-5950-4691-84d8-201171c0ac30\",\n" +
        "                              \"externalizedKeyName\" : \"\",\n" +
        "                              \"configFieldName\" : \"enableHttp2\",\n" +
        "                              \"description\" : \"\",\n" +
        "                              \"externalized\" : false,\n" +
        "                              \"defaultValue\" : false\n" +
        "                            },\n" +
        "                            \"uri\" : {\n" +
        "                              \"type\" : \"string\",\n" +
        "                              \"$id\" : \"04ab34dc-6f39-462e-b861-b1132c1c361f\",\n" +
        "                              \"externalizedKeyName\" : \"\",\n" +
        "                              \"configFieldName\" : \"uri\",\n" +
        "                              \"description\" : \"\",\n" +
        "                              \"externalized\" : false,\n" +
        "                              \"defaultValue\" : \"\",\n" +
        "                              \"minLength\" : 0,\n" +
        "                              \"maxLength\" : 2147483647,\n" +
        "                              \"pattern\" : \"\",\n" +
        "                              \"format\" : \"none\"\n" +
        "                            },\n" +
        "                            \"client_id\" : {\n" +
        "                              \"type\" : \"string\",\n" +
        "                              \"$id\" : \"c24fcd31-743b-4108-bd8b-485d293bea16\",\n" +
        "                              \"externalizedKeyName\" : \"\",\n" +
        "                              \"configFieldName\" : \"client_id\",\n" +
        "                              \"description\" : \"\",\n" +
        "                              \"externalized\" : false,\n" +
        "                              \"defaultValue\" : \"\",\n" +
        "                              \"minLength\" : 0,\n" +
        "                              \"maxLength\" : 2147483647,\n" +
        "                              \"pattern\" : \"\",\n" +
        "                              \"format\" : \"none\"\n" +
        "                            },\n" +
        "                            \"client_secret\" : {\n" +
        "                              \"type\" : \"string\",\n" +
        "                              \"$id\" : \"b78bdb35-1135-4273-964b-85fc479c020f\",\n" +
        "                              \"externalizedKeyName\" : \"\",\n" +
        "                              \"configFieldName\" : \"client_secret\",\n" +
        "                              \"description\" : \"\",\n" +
        "                              \"externalized\" : false,\n" +
        "                              \"defaultValue\" : \"\",\n" +
        "                              \"minLength\" : 0,\n" +
        "                              \"maxLength\" : 2147483647,\n" +
        "                              \"pattern\" : \"\",\n" +
        "                              \"format\" : \"none\"\n" +
        "                            },\n" +
        "                            \"scope\" : {\n" +
        "                              \"type\" : \"array\",\n" +
        "                              \"$id\" : \"2f17c635-8a05-4a5a-bdba-e7ebf3cd27c9\",\n" +
        "                              \"externalizedKeyName\" : \"\",\n" +
        "                              \"configFieldName\" : \"scope\",\n" +
        "                              \"description\" : \"\",\n" +
        "                              \"externalized\" : false,\n" +
        "                              \"items\" : {\n" +
        "                                \"type\" : \"string\"\n" +
        "                              },\n" +
        "                              \"minItems\" : 0,\n" +
        "                              \"maxItems\" : 2147483647,\n" +
        "                              \"uniqueItems\" : false,\n" +
        "                              \"contains\" : false,\n" +
        "                              \"useSubObjectDefault\" : false,\n" +
        "                              \"defaultValue\" : \"\"\n" +
        "                            }\n" +
        "                          },\n" +
        "                          \"type\" : \"object\"\n" +
        "                        },\n" +
        "                        \"defaultValue\" : \"\"\n" +
        "                      }\n" +
        "                    },\n" +
        "                    \"type\" : \"object\"\n" +
        "                  }\n" +
        "                },\n" +
        "                \"refresh_token\" : {\n" +
        "                  \"type\" : \"object\",\n" +
        "                  \"$id\" : \"eacb5bdc-3c63-476b-92ba-454a8ef40738\",\n" +
        "                  \"externalizedKeyName\" : \"\",\n" +
        "                  \"configFieldName\" : \"refresh_token\",\n" +
        "                  \"description\" : \"\",\n" +
        "                  \"externalized\" : false,\n" +
        "                  \"useSubObjectDefault\" : true,\n" +
        "                  \"defaultValue\" : \"\",\n" +
        "                  \"ref\" : {\n" +
        "                    \"properties\" : {\n" +
        "                      \"uri\" : {\n" +
        "                        \"type\" : \"string\",\n" +
        "                        \"$id\" : \"cddc0cd6-5103-4230-9617-d9d0bc0ef16e\",\n" +
        "                        \"externalizedKeyName\" : \"tokenRtUri\",\n" +
        "                        \"configFieldName\" : \"uri\",\n" +
        "                        \"description\" : \"token endpoint for refresh token grant\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"defaultValue\" : \"/oauth2/token\",\n" +
        "                        \"minLength\" : 0,\n" +
        "                        \"maxLength\" : 2147483647,\n" +
        "                        \"pattern\" : \"\",\n" +
        "                        \"format\" : \"none\"\n" +
        "                      },\n" +
        "                      \"client_id\" : {\n" +
        "                        \"type\" : \"string\",\n" +
        "                        \"$id\" : \"f962b710-83b0-4e84-8f56-045284be5222\",\n" +
        "                        \"externalizedKeyName\" : \"tokenRtClientId\",\n" +
        "                        \"configFieldName\" : \"client_id\",\n" +
        "                        \"description\" : \"client_id for refresh token grant flow.\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"defaultValue\" : \"f7d42348-c647-4efb-a52d-4c5787421e72\",\n" +
        "                        \"minLength\" : 0,\n" +
        "                        \"maxLength\" : 2147483647,\n" +
        "                        \"pattern\" : \"\",\n" +
        "                        \"format\" : \"none\"\n" +
        "                      },\n" +
        "                      \"client_secret\" : {\n" +
        "                        \"type\" : \"string\",\n" +
        "                        \"$id\" : \"7eaf5aaf-db5e-4548-ad6a-c5121e6b4ab6\",\n" +
        "                        \"externalizedKeyName\" : \"tokenRtClientSecret\",\n" +
        "                        \"configFieldName\" : \"client_secret\",\n" +
        "                        \"description\" : \"client_secret for refresh token grant flow\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"defaultValue\" : \"f6h1FTI8Q3-7UScPZDzfXA\",\n" +
        "                        \"minLength\" : 0,\n" +
        "                        \"maxLength\" : 2147483647,\n" +
        "                        \"pattern\" : \"\",\n" +
        "                        \"format\" : \"none\"\n" +
        "                      },\n" +
        "                      \"scope\" : {\n" +
        "                        \"type\" : \"array\",\n" +
        "                        \"$id\" : \"75100262-5e14-490e-92d8-f2954249051a\",\n" +
        "                        \"externalizedKeyName\" : \"tokenRtScope\",\n" +
        "                        \"configFieldName\" : \"scope\",\n" +
        "                        \"description\" : \"optional scope, default scope in the client registration will be used if not defined.\\nIf there are scopes specified here, they will be verified against the registered scopes.\\nIn values.yml, you define a list of strings for the scope(s).\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"items\" : {\n" +
        "                          \"type\" : \"string\"\n" +
        "                        },\n" +
        "                        \"minItems\" : 0,\n" +
        "                        \"maxItems\" : 2147483647,\n" +
        "                        \"uniqueItems\" : false,\n" +
        "                        \"contains\" : false,\n" +
        "                        \"useSubObjectDefault\" : false,\n" +
        "                        \"defaultValue\" : \"\"\n" +
        "                      }\n" +
        "                    },\n" +
        "                    \"type\" : \"object\"\n" +
        "                  }\n" +
        "                },\n" +
        "                \"key\" : {\n" +
        "                  \"type\" : \"object\",\n" +
        "                  \"$id\" : \"7bd58358-2bc5-44a0-bcb7-b461e24a19d7\",\n" +
        "                  \"externalizedKeyName\" : \"\",\n" +
        "                  \"configFieldName\" : \"key\",\n" +
        "                  \"description\" : \"light-oauth2 key distribution endpoint configuration for token verification\",\n" +
        "                  \"externalized\" : false,\n" +
        "                  \"useSubObjectDefault\" : true,\n" +
        "                  \"defaultValue\" : \"\",\n" +
        "                  \"ref\" : {\n" +
        "                    \"properties\" : {\n" +
        "                      \"server_url\" : {\n" +
        "                        \"type\" : \"string\",\n" +
        "                        \"$id\" : \"519d2108-8221-484a-9d2d-03021130639e\",\n" +
        "                        \"externalizedKeyName\" : \"tokenKeyServerUrl\",\n" +
        "                        \"configFieldName\" : \"server_url\",\n" +
        "                        \"description\" : \"key distribution server url for token verification. It will be used if it is configured.\\nIf it is not set, a service lookup will be taken with serviceId to find an instance\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"defaultValue\" : \"\",\n" +
        "                        \"minLength\" : 0,\n" +
        "                        \"maxLength\" : 2147483647,\n" +
        "                        \"pattern\" : \"\",\n" +
        "                        \"format\" : \"none\"\n" +
        "                      },\n" +
        "                      \"serviceId\" : {\n" +
        "                        \"type\" : \"string\",\n" +
        "                        \"$id\" : \"d1c34934-5283-4667-acdc-ad5b9d13a06b\",\n" +
        "                        \"externalizedKeyName\" : \"tokenKeyServiceId\",\n" +
        "                        \"configFieldName\" : \"serviceId\",\n" +
        "                        \"description\" : \"key serviceId for key distribution service, it will be used if above server_url is not configured.\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"defaultValue\" : \"com.networknt.oauth2-key-1.0.0\",\n" +
        "                        \"minLength\" : 0,\n" +
        "                        \"maxLength\" : 2147483647,\n" +
        "                        \"pattern\" : \"\",\n" +
        "                        \"format\" : \"none\"\n" +
        "                      },\n" +
        "                      \"uri\" : {\n" +
        "                        \"type\" : \"string\",\n" +
        "                        \"$id\" : \"02c649f0-ef59-45c4-827d-f424733d2480\",\n" +
        "                        \"externalizedKeyName\" : \"tokenKeyUri\",\n" +
        "                        \"configFieldName\" : \"uri\",\n" +
        "                        \"description\" : \"the path for the key distribution endpoint\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"defaultValue\" : \"/oauth2/key\",\n" +
        "                        \"minLength\" : 0,\n" +
        "                        \"maxLength\" : 2147483647,\n" +
        "                        \"pattern\" : \"\",\n" +
        "                        \"format\" : \"none\"\n" +
        "                      },\n" +
        "                      \"client_id\" : {\n" +
        "                        \"type\" : \"string\",\n" +
        "                        \"$id\" : \"e0f367b3-44c6-4a55-ac45-0e870646fafd\",\n" +
        "                        \"externalizedKeyName\" : \"tokenKeyClientId\",\n" +
        "                        \"configFieldName\" : \"client_id\",\n" +
        "                        \"description\" : \"client_id used to access key distribution service. It can be the same client_id with token service or not.\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"defaultValue\" : \"f7d42348-c647-4efb-a52d-4c5787421e72\",\n" +
        "                        \"minLength\" : 0,\n" +
        "                        \"maxLength\" : 2147483647,\n" +
        "                        \"pattern\" : \"\",\n" +
        "                        \"format\" : \"none\"\n" +
        "                      },\n" +
        "                      \"client_secret\" : {\n" +
        "                        \"type\" : \"string\",\n" +
        "                        \"$id\" : \"3e81ae09-2cca-4df8-ad2d-e74591922db6\",\n" +
        "                        \"externalizedKeyName\" : \"tokenKeyClientSecret\",\n" +
        "                        \"configFieldName\" : \"client_secret\",\n" +
        "                        \"description\" : \"client secret used to access the key distribution service.\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"defaultValue\" : \"f6h1FTI8Q3-7UScPZDzfXA\",\n" +
        "                        \"minLength\" : 0,\n" +
        "                        \"maxLength\" : 2147483647,\n" +
        "                        \"pattern\" : \"\",\n" +
        "                        \"format\" : \"none\"\n" +
        "                      },\n" +
        "                      \"enableHttp2\" : {\n" +
        "                        \"type\" : \"boolean\",\n" +
        "                        \"$id\" : \"e951c46f-29b8-4e6a-8bb1-807708723373\",\n" +
        "                        \"externalizedKeyName\" : \"tokenKeyEnableHttp2\",\n" +
        "                        \"configFieldName\" : \"enableHttp2\",\n" +
        "                        \"description\" : \"set to true if the oauth2 provider supports HTTP/2\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"defaultValue\" : true\n" +
        "                      },\n" +
        "                      \"serviceIdAuthServers\" : {\n" +
        "                        \"type\" : \"map\",\n" +
        "                        \"$id\" : \"fc751996-c1ef-40e8-a286-d587d14988cd\",\n" +
        "                        \"externalizedKeyName\" : \"tokenKeyServiceIdAuthServers\",\n" +
        "                        \"configFieldName\" : \"serviceIdAuthServers\",\n" +
        "                        \"description\" : \"The serviceId to the service specific OAuth 2.0 configuration. Used only when multipleOAuthServer is\\nset as true. For detailed config options, please see the values.yml in the client module test.\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"additionalProperties\" : {\n" +
        "                          \"properties\" : {\n" +
        "                            \"server_url\" : {\n" +
        "                              \"type\" : \"string\",\n" +
        "                              \"$id\" : \"69a9c422-e39b-4b8e-abce-e345e0bb063f\",\n" +
        "                              \"externalizedKeyName\" : \"\",\n" +
        "                              \"configFieldName\" : \"server_url\",\n" +
        "                              \"description\" : \"\",\n" +
        "                              \"externalized\" : false,\n" +
        "                              \"defaultValue\" : \"\",\n" +
        "                              \"minLength\" : 0,\n" +
        "                              \"maxLength\" : 2147483647,\n" +
        "                              \"pattern\" : \"\",\n" +
        "                              \"format\" : \"none\"\n" +
        "                            },\n" +
        "                            \"enableHttp2\" : {\n" +
        "                              \"type\" : \"boolean\",\n" +
        "                              \"$id\" : \"46d5891e-d9d0-4357-a380-d6ad44dfbbcd\",\n" +
        "                              \"externalizedKeyName\" : \"\",\n" +
        "                              \"configFieldName\" : \"enableHttp2\",\n" +
        "                              \"description\" : \"\",\n" +
        "                              \"externalized\" : false,\n" +
        "                              \"defaultValue\" : false\n" +
        "                            },\n" +
        "                            \"uri\" : {\n" +
        "                              \"type\" : \"string\",\n" +
        "                              \"$id\" : \"142a9d18-7733-4ce4-b48e-ddf2c7a5dc6f\",\n" +
        "                              \"externalizedKeyName\" : \"\",\n" +
        "                              \"configFieldName\" : \"uri\",\n" +
        "                              \"description\" : \"\",\n" +
        "                              \"externalized\" : false,\n" +
        "                              \"defaultValue\" : \"\",\n" +
        "                              \"minLength\" : 0,\n" +
        "                              \"maxLength\" : 2147483647,\n" +
        "                              \"pattern\" : \"\",\n" +
        "                              \"format\" : \"none\"\n" +
        "                            },\n" +
        "                            \"client_id\" : {\n" +
        "                              \"type\" : \"string\",\n" +
        "                              \"$id\" : \"5a4a0fc6-899b-4abb-99e1-0efd9d7021ae\",\n" +
        "                              \"externalizedKeyName\" : \"\",\n" +
        "                              \"configFieldName\" : \"client_id\",\n" +
        "                              \"description\" : \"\",\n" +
        "                              \"externalized\" : false,\n" +
        "                              \"defaultValue\" : \"\",\n" +
        "                              \"minLength\" : 0,\n" +
        "                              \"maxLength\" : 2147483647,\n" +
        "                              \"pattern\" : \"\",\n" +
        "                              \"format\" : \"none\"\n" +
        "                            },\n" +
        "                            \"client_secret\" : {\n" +
        "                              \"type\" : \"string\",\n" +
        "                              \"$id\" : \"4c0a7412-2987-4f72-bded-a5b4c9b87b08\",\n" +
        "                              \"externalizedKeyName\" : \"\",\n" +
        "                              \"configFieldName\" : \"client_secret\",\n" +
        "                              \"description\" : \"\",\n" +
        "                              \"externalized\" : false,\n" +
        "                              \"defaultValue\" : \"\",\n" +
        "                              \"minLength\" : 0,\n" +
        "                              \"maxLength\" : 2147483647,\n" +
        "                              \"pattern\" : \"\",\n" +
        "                              \"format\" : \"none\"\n" +
        "                            },\n" +
        "                            \"scope\" : {\n" +
        "                              \"type\" : \"array\",\n" +
        "                              \"$id\" : \"12d79d68-f12a-4248-a221-f9c4c39523be\",\n" +
        "                              \"externalizedKeyName\" : \"\",\n" +
        "                              \"configFieldName\" : \"scope\",\n" +
        "                              \"description\" : \"\",\n" +
        "                              \"externalized\" : false,\n" +
        "                              \"items\" : {\n" +
        "                                \"type\" : \"string\"\n" +
        "                              },\n" +
        "                              \"minItems\" : 0,\n" +
        "                              \"maxItems\" : 2147483647,\n" +
        "                              \"uniqueItems\" : false,\n" +
        "                              \"contains\" : false,\n" +
        "                              \"useSubObjectDefault\" : false,\n" +
        "                              \"defaultValue\" : \"\"\n" +
        "                            }\n" +
        "                          },\n" +
        "                          \"type\" : \"object\"\n" +
        "                        },\n" +
        "                        \"defaultValue\" : \"\"\n" +
        "                      },\n" +
        "                      \"audience\" : {\n" +
        "                        \"type\" : \"string\",\n" +
        "                        \"$id\" : \"99e86e62-c331-4543-b2f6-bed83ded7152\",\n" +
        "                        \"externalizedKeyName\" : \"tokenKeyAudience\",\n" +
        "                        \"configFieldName\" : \"audience\",\n" +
        "                        \"description\" : \"audience for the token validation. It is optional and if it is not configured, no audience validation will be executed.\",\n" +
        "                        \"externalized\" : true,\n" +
        "                        \"defaultValue\" : \"\",\n" +
        "                        \"minLength\" : 0,\n" +
        "                        \"maxLength\" : 2147483647,\n" +
        "                        \"pattern\" : \"\",\n" +
        "                        \"format\" : \"none\"\n" +
        "                      }\n" +
        "                    },\n" +
        "                    \"type\" : \"object\"\n" +
        "                  }\n" +
        "                }\n" +
        "              },\n" +
        "              \"type\" : \"object\"\n" +
        "            }\n" +
        "          },\n" +
        "          \"sign\" : {\n" +
        "            \"type\" : \"object\",\n" +
        "            \"$id\" : \"feb43cf6-de5e-418c-a449-94f8c1607600\",\n" +
        "            \"externalizedKeyName\" : \"\",\n" +
        "            \"configFieldName\" : \"sign\",\n" +
        "            \"description\" : \"Sign endpoint configuration\",\n" +
        "            \"externalized\" : false,\n" +
        "            \"useSubObjectDefault\" : true,\n" +
        "            \"defaultValue\" : \"\",\n" +
        "            \"ref\" : {\n" +
        "              \"properties\" : {\n" +
        "                \"server_url\" : {\n" +
        "                  \"type\" : \"string\",\n" +
        "                  \"$id\" : \"fa2382c5-91c9-4f97-8d24-b4163bea1add\",\n" +
        "                  \"externalizedKeyName\" : \"signKeyServerUrl\",\n" +
        "                  \"configFieldName\" : \"server_url\",\n" +
        "                  \"description\" : \"key distribution server url. It will be used to establish connection if it exists.\\nif it is not set, then a service lookup against serviceId will be taken to discover an instance.\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : \"\",\n" +
        "                  \"minLength\" : 0,\n" +
        "                  \"maxLength\" : 2147483647,\n" +
        "                  \"pattern\" : \"\",\n" +
        "                  \"format\" : \"none\"\n" +
        "                },\n" +
        "                \"serviceId\" : {\n" +
        "                  \"type\" : \"string\",\n" +
        "                  \"$id\" : \"e33769c7-1719-488e-af2a-a5494a21b9f6\",\n" +
        "                  \"externalizedKeyName\" : \"signKeyServiceId\",\n" +
        "                  \"configFieldName\" : \"serviceId\",\n" +
        "                  \"description\" : \"the unique service id for key distribution service, it will be used to lookup key service if above url doesn't exist.\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : \"com.networknt.oauth2-key-1.0.0\",\n" +
        "                  \"minLength\" : 0,\n" +
        "                  \"maxLength\" : 2147483647,\n" +
        "                  \"pattern\" : \"\",\n" +
        "                  \"format\" : \"none\"\n" +
        "                },\n" +
        "                \"uri\" : {\n" +
        "                  \"type\" : \"string\",\n" +
        "                  \"$id\" : \"bba847ee-4ad0-439c-a694-bb0905c3f218\",\n" +
        "                  \"externalizedKeyName\" : \"signKeyUri\",\n" +
        "                  \"configFieldName\" : \"uri\",\n" +
        "                  \"description\" : \"the path for the key distribution endpoint\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : \"/oauth2/key\",\n" +
        "                  \"minLength\" : 0,\n" +
        "                  \"maxLength\" : 2147483647,\n" +
        "                  \"pattern\" : \"\",\n" +
        "                  \"format\" : \"none\"\n" +
        "                },\n" +
        "                \"client_id\" : {\n" +
        "                  \"type\" : \"string\",\n" +
        "                  \"$id\" : \"59ca3c71-526c-4d15-94d9-1f9d90185502\",\n" +
        "                  \"externalizedKeyName\" : \"signKeyClientId\",\n" +
        "                  \"configFieldName\" : \"client_id\",\n" +
        "                  \"description\" : \"client_id used to access key distribution service. It can be the same client_id with token service or not.\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : \"f7d42348-c647-4efb-a52d-4c5787421e72\",\n" +
        "                  \"minLength\" : 0,\n" +
        "                  \"maxLength\" : 2147483647,\n" +
        "                  \"pattern\" : \"\",\n" +
        "                  \"format\" : \"none\"\n" +
        "                },\n" +
        "                \"client_secret\" : {\n" +
        "                  \"type\" : \"string\",\n" +
        "                  \"$id\" : \"a69e781d-1429-464e-8da0-93d260996f42\",\n" +
        "                  \"externalizedKeyName\" : \"signKeyClientSecret\",\n" +
        "                  \"configFieldName\" : \"client_secret\",\n" +
        "                  \"description\" : \"client secret used to access the key distribution service.\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : \"f6h1FTI8Q3-7UScPZDzfXA\",\n" +
        "                  \"minLength\" : 0,\n" +
        "                  \"maxLength\" : 2147483647,\n" +
        "                  \"pattern\" : \"\",\n" +
        "                  \"format\" : \"none\"\n" +
        "                },\n" +
        "                \"enableHttp2\" : {\n" +
        "                  \"type\" : \"boolean\",\n" +
        "                  \"$id\" : \"908e74b0-47b6-4647-bf1e-a7990d65deb8\",\n" +
        "                  \"externalizedKeyName\" : \"signKeyEnableHttp2\",\n" +
        "                  \"configFieldName\" : \"enableHttp2\",\n" +
        "                  \"description\" : \"set to true if the oauth2 provider supports HTTP/2\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : true\n" +
        "                },\n" +
        "                \"audience\" : {\n" +
        "                  \"type\" : \"string\",\n" +
        "                  \"$id\" : \"e26a17e2-cd1c-4ee0-b0b6-9677e946c4e3\",\n" +
        "                  \"externalizedKeyName\" : \"signKeyAudience\",\n" +
        "                  \"configFieldName\" : \"audience\",\n" +
        "                  \"description\" : \"audience for the token validation. It is optional and if it is not configured, no audience validation will be executed.\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : \"\",\n" +
        "                  \"minLength\" : 0,\n" +
        "                  \"maxLength\" : 2147483647,\n" +
        "                  \"pattern\" : \"\",\n" +
        "                  \"format\" : \"none\"\n" +
        "                }\n" +
        "              },\n" +
        "              \"type\" : \"object\"\n" +
        "            }\n" +
        "          },\n" +
        "          \"deref\" : {\n" +
        "            \"type\" : \"object\",\n" +
        "            \"$id\" : \"0fb0551b-f090-4e62-b1b1-38a5a8b048fe\",\n" +
        "            \"externalizedKeyName\" : \"\",\n" +
        "            \"configFieldName\" : \"deref\",\n" +
        "            \"description\" : \"de-ref by reference token to JWT token. It is separate service as it might be the external OAuth 2.0 provider.\",\n" +
        "            \"externalized\" : false,\n" +
        "            \"useSubObjectDefault\" : true,\n" +
        "            \"defaultValue\" : \"\",\n" +
        "            \"ref\" : {\n" +
        "              \"properties\" : {\n" +
        "                \"server_url\" : {\n" +
        "                  \"type\" : \"string\",\n" +
        "                  \"$id\" : \"2dd69088-314a-4d3f-a143-f276b4428de9\",\n" +
        "                  \"externalizedKeyName\" : \"derefServerUrl\",\n" +
        "                  \"configFieldName\" : \"server_url\",\n" +
        "                  \"description\" : \"Token service server url, this might be different than the above token server url.\\nThe static url will be used if it is configured.\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : \"\",\n" +
        "                  \"minLength\" : 0,\n" +
        "                  \"maxLength\" : 2147483647,\n" +
        "                  \"pattern\" : \"\",\n" +
        "                  \"format\" : \"none\"\n" +
        "                },\n" +
        "                \"proxyHost\" : {\n" +
        "                  \"type\" : \"string\",\n" +
        "                  \"$id\" : \"4a6c4f74-c282-4e59-83de-a98eccdf4dc1\",\n" +
        "                  \"externalizedKeyName\" : \"derefProxyHost\",\n" +
        "                  \"configFieldName\" : \"proxyHost\",\n" +
        "                  \"description\" : \"For users who leverage SaaS OAuth 2.0 provider in the public cloud and has an internal\\nproxy server to access code, token and key services of OAuth 2.0, set up the proxyHost\\nhere for the HTTPS traffic. This option is only working with server_url and serviceId\\nbelow should be commented out. OAuth 2.0 services cannot be discovered if a proxy is used.\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : \"\",\n" +
        "                  \"minLength\" : 0,\n" +
        "                  \"maxLength\" : 2147483647,\n" +
        "                  \"pattern\" : \"\",\n" +
        "                  \"format\" : \"none\"\n" +
        "                },\n" +
        "                \"proxyPort\" : {\n" +
        "                  \"type\" : \"integer\",\n" +
        "                  \"$id\" : \"de01f75c-386c-4d84-813d-1405ac889d5b\",\n" +
        "                  \"externalizedKeyName\" : \"derefProxyPort\",\n" +
        "                  \"configFieldName\" : \"proxyPort\",\n" +
        "                  \"description\" : \"We only support HTTPS traffic for the proxy and the default port is 443. If your proxy server has\\na different port, please specify it here. If proxyHost is available and proxyPort is missing, then\\nthe default value 443 is going to be used for the HTTP connection.\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : 0,\n" +
        "                  \"minimum\" : -2147483648,\n" +
        "                  \"maximum\" : 2147483647,\n" +
        "                  \"exclusiveMin\" : false,\n" +
        "                  \"exclusiveMax\" : false,\n" +
        "                  \"multipleOf\" : 0,\n" +
        "                  \"format\" : \"int32\"\n" +
        "                },\n" +
        "                \"serviceId\" : {\n" +
        "                  \"type\" : \"string\",\n" +
        "                  \"$id\" : \"2f900228-4983-446b-b696-c42fe81961d5\",\n" +
        "                  \"externalizedKeyName\" : \"derefServiceId\",\n" +
        "                  \"configFieldName\" : \"serviceId\",\n" +
        "                  \"description\" : \"token service unique id for OAuth 2.0 provider. Need for service lookup/discovery. It will be used if above server_url is not configured.\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : \"com.networknt.oauth2-token-1.0.0\",\n" +
        "                  \"minLength\" : 0,\n" +
        "                  \"maxLength\" : 2147483647,\n" +
        "                  \"pattern\" : \"\",\n" +
        "                  \"format\" : \"none\"\n" +
        "                },\n" +
        "                \"enableHttp2\" : {\n" +
        "                  \"type\" : \"boolean\",\n" +
        "                  \"$id\" : \"093b4617-db80-4a84-ab72-bfe57a1b99fd\",\n" +
        "                  \"externalizedKeyName\" : \"derefEnableHttp2\",\n" +
        "                  \"configFieldName\" : \"enableHttp2\",\n" +
        "                  \"description\" : \"set to true if the oauth2 provider supports HTTP/2\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : true\n" +
        "                },\n" +
        "                \"uri\" : {\n" +
        "                  \"type\" : \"string\",\n" +
        "                  \"$id\" : \"195d0fd2-c15e-4892-9e88-edda7315e35d\",\n" +
        "                  \"externalizedKeyName\" : \"derefUri\",\n" +
        "                  \"configFieldName\" : \"uri\",\n" +
        "                  \"description\" : \"the path for the key distribution endpoint\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : \"/oauth2/deref\",\n" +
        "                  \"minLength\" : 0,\n" +
        "                  \"maxLength\" : 2147483647,\n" +
        "                  \"pattern\" : \"\",\n" +
        "                  \"format\" : \"none\"\n" +
        "                },\n" +
        "                \"clientId\" : {\n" +
        "                  \"type\" : \"string\",\n" +
        "                  \"$id\" : \"e69cd0c9-72f8-48a7-98be-4137eebb2584\",\n" +
        "                  \"externalizedKeyName\" : \"derefClientId\",\n" +
        "                  \"configFieldName\" : \"clientId\",\n" +
        "                  \"description\" : \"client_id used to access key distribution service. It can be the same client_id with token service or not.\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : \"f7d42348-c647-4efb-a52d-4c5787421e72\",\n" +
        "                  \"minLength\" : 0,\n" +
        "                  \"maxLength\" : 2147483647,\n" +
        "                  \"pattern\" : \"\",\n" +
        "                  \"format\" : \"none\"\n" +
        "                },\n" +
        "                \"clientSecret\" : {\n" +
        "                  \"type\" : \"string\",\n" +
        "                  \"$id\" : \"1dc72b75-bd1c-419e-a57a-066640db079a\",\n" +
        "                  \"externalizedKeyName\" : \"derefClientSecret\",\n" +
        "                  \"configFieldName\" : \"clientSecret\",\n" +
        "                  \"description\" : \"client_secret for deref\",\n" +
        "                  \"externalized\" : true,\n" +
        "                  \"defaultValue\" : \"f6h1FTI8Q3-7UScPZDzfXA\",\n" +
        "                  \"minLength\" : 0,\n" +
        "                  \"maxLength\" : 2147483647,\n" +
        "                  \"pattern\" : \"\",\n" +
        "                  \"format\" : \"none\"\n" +
        "                }\n" +
        "              },\n" +
        "              \"type\" : \"object\"\n" +
        "            }\n" +
        "          }\n" +
        "        },\n" +
        "        \"type\" : \"object\"\n" +
        "      }\n" +
        "    },\n" +
        "    \"pathPrefixServices\" : {\n" +
        "      \"type\" : \"map\",\n" +
        "      \"$id\" : \"cdcc53dd-e4ca-42bd-90a6-54f9fdeb54a7\",\n" +
        "      \"externalizedKeyName\" : \"\",\n" +
        "      \"configFieldName\" : \"pathPrefixServices\",\n" +
        "      \"description\" : \"If you have multiple OAuth 2.0 providers and use path prefix to decide which OAuth 2.0 server\\nto get the token or JWK. If two or more services have the same path, you must use serviceId in\\nthe request header to use the serviceId to find the OAuth 2.0 provider configuration.\",\n" +
        "      \"externalized\" : true,\n" +
        "      \"additionalProperties\" : {\n" +
        "        \"type\" : \"string\"\n" +
        "      },\n" +
        "      \"defaultValue\" : \"\"\n" +
        "    },\n" +
        "    \"request\" : {\n" +
        "      \"type\" : \"object\",\n" +
        "      \"$id\" : \"31e0fab1-14a5-4306-8ba9-3b362dfe3671\",\n" +
        "      \"externalizedKeyName\" : \"\",\n" +
        "      \"configFieldName\" : \"request\",\n" +
        "      \"description\" : \"Circuit breaker configuration for the client\",\n" +
        "      \"externalized\" : false,\n" +
        "      \"useSubObjectDefault\" : true,\n" +
        "      \"defaultValue\" : \"\",\n" +
        "      \"ref\" : {\n" +
        "        \"properties\" : {\n" +
        "          \"errorThreshold\" : {\n" +
        "            \"type\" : \"integer\",\n" +
        "            \"$id\" : \"029dd44f-11e1-45ec-8b3e-660a3b315256\",\n" +
        "            \"externalizedKeyName\" : \"errorThreshold\",\n" +
        "            \"configFieldName\" : \"errorThreshold\",\n" +
        "            \"description\" : \"number of timeouts/errors to break the circuit\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : 2,\n" +
        "            \"minimum\" : -2147483648,\n" +
        "            \"maximum\" : 2147483647,\n" +
        "            \"exclusiveMin\" : false,\n" +
        "            \"exclusiveMax\" : false,\n" +
        "            \"multipleOf\" : 0,\n" +
        "            \"format\" : \"int32\"\n" +
        "          },\n" +
        "          \"timeout\" : {\n" +
        "            \"type\" : \"integer\",\n" +
        "            \"$id\" : \"b8112fc7-460f-495b-84cb-b565529e3847\",\n" +
        "            \"externalizedKeyName\" : \"timeout\",\n" +
        "            \"configFieldName\" : \"timeout\",\n" +
        "            \"description\" : \"timeout in millisecond to indicate a client error. If light-4j Http2Client is used, it is the timeout to get the\\nconnection. If http-client (JDK 11 client wrapper) is used, it is the request timeout.\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : 3000,\n" +
        "            \"minimum\" : -2147483648,\n" +
        "            \"maximum\" : 2147483647,\n" +
        "            \"exclusiveMin\" : false,\n" +
        "            \"exclusiveMax\" : false,\n" +
        "            \"multipleOf\" : 0,\n" +
        "            \"format\" : \"int32\"\n" +
        "          },\n" +
        "          \"resetTimeout\" : {\n" +
        "            \"type\" : \"integer\",\n" +
        "            \"$id\" : \"6dab7d33-543d-4b1c-9b16-86e98bd71e04\",\n" +
        "            \"externalizedKeyName\" : \"resetTimeout\",\n" +
        "            \"configFieldName\" : \"resetTimeout\",\n" +
        "            \"description\" : \"reset the circuit after this timeout in millisecond\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : 7000,\n" +
        "            \"minimum\" : -2147483648,\n" +
        "            \"maximum\" : 2147483647,\n" +
        "            \"exclusiveMin\" : false,\n" +
        "            \"exclusiveMax\" : false,\n" +
        "            \"multipleOf\" : 0,\n" +
        "            \"format\" : \"int32\"\n" +
        "          },\n" +
        "          \"injectOpenTracing\" : {\n" +
        "            \"type\" : \"boolean\",\n" +
        "            \"$id\" : \"ca8f6f03-9ed8-423d-aa2b-fcd4d7b2ba0d\",\n" +
        "            \"externalizedKeyName\" : \"injectOpenTracing\",\n" +
        "            \"configFieldName\" : \"injectOpenTracing\",\n" +
        "            \"description\" : \"if open tracing is enabled. traceability, correlation and metrics should not be in the chain if opentracing is used.\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : false\n" +
        "          },\n" +
        "          \"injectCallerId\" : {\n" +
        "            \"type\" : \"boolean\",\n" +
        "            \"$id\" : \"262434f0-da76-4a39-af62-57dc757d2481\",\n" +
        "            \"externalizedKeyName\" : \"injectCallerId\",\n" +
        "            \"configFieldName\" : \"injectCallerId\",\n" +
        "            \"description\" : \"inject serviceId as callerId into the http header for metrics to collect the caller. The serviceId is from server.yml\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : false\n" +
        "          },\n" +
        "          \"enableHttp2\" : {\n" +
        "            \"type\" : \"boolean\",\n" +
        "            \"$id\" : \"1137c5c3-1c93-4b1a-92d3-5f714e749528\",\n" +
        "            \"externalizedKeyName\" : \"enableHttp2\",\n" +
        "            \"configFieldName\" : \"enableHttp2\",\n" +
        "            \"description\" : \"the flag to indicate whether http/2 is enabled when calling client.callService()\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : true\n" +
        "          },\n" +
        "          \"connectionPoolSize\" : {\n" +
        "            \"type\" : \"integer\",\n" +
        "            \"$id\" : \"4573f6ce-303f-4ca1-bce9-f8e02bbfedfa\",\n" +
        "            \"externalizedKeyName\" : \"connectionPoolSize\",\n" +
        "            \"configFieldName\" : \"connectionPoolSize\",\n" +
        "            \"description\" : \"the maximum host capacity of connection pool\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : 1000,\n" +
        "            \"minimum\" : -2147483648,\n" +
        "            \"maximum\" : 2147483647,\n" +
        "            \"exclusiveMin\" : false,\n" +
        "            \"exclusiveMax\" : false,\n" +
        "            \"multipleOf\" : 0,\n" +
        "            \"format\" : \"int32\"\n" +
        "          },\n" +
        "          \"connectionExpireTime\" : {\n" +
        "            \"type\" : \"integer\",\n" +
        "            \"$id\" : \"7bbe96a7-d2b2-42b2-9441-660ecb960ab1\",\n" +
        "            \"externalizedKeyName\" : \"connectionExpireTime\",\n" +
        "            \"configFieldName\" : \"connectionExpireTime\",\n" +
        "            \"description\" : \"Connection expire time when connection pool is used. By default, the cached connection will be closed after 30 minutes.\\nThis is one way to force the connection to be closed so that the client-side discovery can be balanced.\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : 1800000,\n" +
        "            \"minimum\" : -2147483648,\n" +
        "            \"maximum\" : 2147483647,\n" +
        "            \"exclusiveMin\" : false,\n" +
        "            \"exclusiveMax\" : false,\n" +
        "            \"multipleOf\" : 0,\n" +
        "            \"format\" : \"int32\"\n" +
        "          },\n" +
        "          \"maxReqPerConn\" : {\n" +
        "            \"type\" : \"integer\",\n" +
        "            \"$id\" : \"a73b1bd3-f54d-44e7-91f1-2091c23eff22\",\n" +
        "            \"externalizedKeyName\" : \"maxReqPerConn\",\n" +
        "            \"configFieldName\" : \"maxReqPerConn\",\n" +
        "            \"description\" : \"The maximum request limitation for each connection in the connection pool. By default, a connection will be closed after\\nsending 1 million requests. This is one way to force the client-side discovery to re-balance the connections.\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : 1000000,\n" +
        "            \"minimum\" : -2147483648,\n" +
        "            \"maximum\" : 2147483647,\n" +
        "            \"exclusiveMin\" : false,\n" +
        "            \"exclusiveMax\" : false,\n" +
        "            \"multipleOf\" : 0,\n" +
        "            \"format\" : \"int32\"\n" +
        "          },\n" +
        "          \"maxConnectionNumPerHost\" : {\n" +
        "            \"type\" : \"integer\",\n" +
        "            \"$id\" : \"0322da0f-08ab-49db-938a-9e854c2b2875\",\n" +
        "            \"externalizedKeyName\" : \"maxConnectionNumPerHost\",\n" +
        "            \"configFieldName\" : \"maxConnectionNumPerHost\",\n" +
        "            \"description\" : \"maximum quantity of connection in connection pool for each host\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : 1000,\n" +
        "            \"minimum\" : -2147483648,\n" +
        "            \"maximum\" : 2147483647,\n" +
        "            \"exclusiveMin\" : false,\n" +
        "            \"exclusiveMax\" : false,\n" +
        "            \"multipleOf\" : 0,\n" +
        "            \"format\" : \"int32\"\n" +
        "          },\n" +
        "          \"minConnectionNumPerHost\" : {\n" +
        "            \"type\" : \"integer\",\n" +
        "            \"$id\" : \"919cfff3-a87d-4e09-a518-bf157fd34fbc\",\n" +
        "            \"externalizedKeyName\" : \"minConnectionNumPerHost\",\n" +
        "            \"configFieldName\" : \"minConnectionNumPerHost\",\n" +
        "            \"description\" : \"minimum quantity of connection in connection pool for each host. The corresponding connection number will shrink to minConnectionNumPerHost\\nby remove least recently used connections when the connection number of a host reach 0.75 * maxConnectionNumPerHost.\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : 250,\n" +
        "            \"minimum\" : -2147483648,\n" +
        "            \"maximum\" : 2147483647,\n" +
        "            \"exclusiveMin\" : false,\n" +
        "            \"exclusiveMax\" : false,\n" +
        "            \"multipleOf\" : 0,\n" +
        "            \"format\" : \"int32\"\n" +
        "          },\n" +
        "          \"maxRequestRetry\" : {\n" +
        "            \"type\" : \"integer\",\n" +
        "            \"$id\" : \"dc4ab1da-8915-4e02-81f2-6cefff265bae\",\n" +
        "            \"externalizedKeyName\" : \"maxRequestRetry\",\n" +
        "            \"configFieldName\" : \"maxRequestRetry\",\n" +
        "            \"description\" : \"Maximum request retry times for each request. If you don't want to retry, set it to 1.\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : 3,\n" +
        "            \"minimum\" : -2147483648,\n" +
        "            \"maximum\" : 2147483647,\n" +
        "            \"exclusiveMin\" : false,\n" +
        "            \"exclusiveMax\" : false,\n" +
        "            \"multipleOf\" : 0,\n" +
        "            \"format\" : \"int32\"\n" +
        "          },\n" +
        "          \"requestRetryDelay\" : {\n" +
        "            \"type\" : \"integer\",\n" +
        "            \"$id\" : \"af57c8cb-fc3d-44ba-a829-e0d44ff27676\",\n" +
        "            \"externalizedKeyName\" : \"requestRetryDelay\",\n" +
        "            \"configFieldName\" : \"requestRetryDelay\",\n" +
        "            \"description\" : \"The delay time in milliseconds for each request retry.\",\n" +
        "            \"externalized\" : true,\n" +
        "            \"defaultValue\" : 1000,\n" +
        "            \"minimum\" : -2147483648,\n" +
        "            \"maximum\" : 2147483647,\n" +
        "            \"exclusiveMin\" : false,\n" +
        "            \"exclusiveMax\" : false,\n" +
        "            \"multipleOf\" : 0,\n" +
        "            \"format\" : \"int32\"\n" +
        "          }\n" +
        "        },\n" +
        "        \"type\" : \"object\"\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"type\" : \"object\"\n" +
        "}";
    public static final String basicAuthMetadata = "{\n" +
            "  \"properties\" : {\n" +
            "    \"enabled\" : {\n" +
            "      \"type\" : \"boolean\",\n" +
            "      \"$id\" : \"e1a2e703-5592-443f-bd57-b504442e72c8\",\n" +
            "      \"externalizedKeyName\" : \"enabled\",\n" +
            "      \"configFieldName\" : \"enabled\",\n" +
            "      \"description\" : \"Enable Basic Authentication Handler, default is true.\",\n" +
            "      \"externalized\" : true,\n" +
            "      \"defaultValue\" : \"false\"\n" +
            "    },\n" +
            "    \"enableAD\" : {\n" +
            "      \"type\" : \"boolean\",\n" +
            "      \"$id\" : \"2734be98-4a5d-4b03-868b-c7b90bf831e0\",\n" +
            "      \"externalizedKeyName\" : \"enableAD\",\n" +
            "      \"configFieldName\" : \"enableAD\",\n" +
            "      \"description\" : \"Enable Ldap Authentication, default is true.\",\n" +
            "      \"externalized\" : true,\n" +
            "      \"defaultValue\" : \"true\"\n" +
            "    },\n" +
            "    \"allowAnonymous\" : {\n" +
            "      \"type\" : \"boolean\",\n" +
            "      \"$id\" : \"4583486d-ad3b-4a55-aeef-8d3185965f0c\",\n" +
            "      \"externalizedKeyName\" : \"allowAnonymous\",\n" +
            "      \"configFieldName\" : \"allowAnonymous\",\n" +
            "      \"description\" : \"Do we allow the anonymous to pass the authentication and limit it with some paths\\nto access? Default is false, and it should only be true in client-proxy.\",\n" +
            "      \"externalized\" : true,\n" +
            "      \"defaultValue\" : \"\"\n" +
            "    },\n" +
            "    \"allowBearerToken\" : {\n" +
            "      \"type\" : \"boolean\",\n" +
            "      \"$id\" : \"cf161f78-8832-49a0-af71-0bf57d1a1185\",\n" +
            "      \"externalizedKeyName\" : \"allowBearerToken\",\n" +
            "      \"configFieldName\" : \"allowBearerToken\",\n" +
            "      \"description\" : \"Allow the Bearer OAuth 2.0 token authorization to pass to the next handler with paths\\nauthorization defined under username bearer. This feature is used in proxy-client\\nthat support multiple clients with different authorizations.\\n\",\n" +
            "      \"externalized\" : true,\n" +
            "      \"defaultValue\" : \"\"\n" +
            "    },\n" +
            "    \"users\" : {\n" +
            "      \"ref\" : {\n" +
            "        \"properties\" : {\n" +
            "          \"username\" : {\n" +
            "            \"type\" : \"string\",\n" +
            "            \"$id\" : \"97a737ed-22eb-4c77-b693-ec60ab8d77b0\",\n" +
            "            \"externalizedKeyName\" : \"\",\n" +
            "            \"configFieldName\" : \"username\",\n" +
            "            \"description\" : \"UserAuth username\",\n" +
            "            \"externalized\" : false,\n" +
            "            \"defaultValue\" : \"\",\n" +
            "            \"minLength\" : 0,\n" +
            "            \"maxLength\" : 2147483647,\n" +
            "            \"pattern\" : \"\",\n" +
            "            \"format\" : \"none\"\n" +
            "          },\n" +
            "          \"password\" : {\n" +
            "            \"type\" : \"string\",\n" +
            "            \"$id\" : \"97dcc8fb-80d3-4702-aa14-f5aa81bfd62c\",\n" +
            "            \"externalizedKeyName\" : \"\",\n" +
            "            \"configFieldName\" : \"password\",\n" +
            "            \"description\" : \"UserAuth password\",\n" +
            "            \"externalized\" : false,\n" +
            "            \"defaultValue\" : \"\",\n" +
            "            \"minLength\" : 0,\n" +
            "            \"maxLength\" : 2147483647,\n" +
            "            \"pattern\" : \"\",\n" +
            "            \"format\" : \"none\"\n" +
            "          },\n" +
            "          \"paths\" : {\n" +
            "            \"ref\" : {\n" +
            "              \"type\" : \"string\"\n" +
            "            },\n" +
            "            \"type\" : \"array\",\n" +
            "            \"$id\" : \"102cb7e0-fd01-4196-a573-7fb97913aaf2\",\n" +
            "            \"externalizedKeyName\" : \"\",\n" +
            "            \"configFieldName\" : \"paths\",\n" +
            "            \"description\" : \"The different paths that will be valid for this UserAuth\",\n" +
            "            \"externalized\" : false,\n" +
            "            \"minItems\" : 0,\n" +
            "            \"maxItems\" : 2147483647,\n" +
            "            \"uniqueItems\" : false,\n" +
            "            \"contains\" : false,\n" +
            "            \"useSubObjectDefault\" : false,\n" +
            "            \"defaultValue\" : \"\"\n" +
            "          }\n" +
            "        },\n" +
            "        \"type\" : \"object\"\n" +
            "      },\n" +
            "      \"type\" : \"map\",\n" +
            "      \"$id\" : \"b55f7b34-2cd9-450f-a9cb-604abe90d343\",\n" +
            "      \"externalizedKeyName\" : \"users\",\n" +
            "      \"configFieldName\" : \"users\",\n" +
            "      \"description\" : \"usernames and passwords in a list, the password can be encrypted like user2 in test.\\nAs we are supporting multiple users, so leave the passwords in this file with users.\\nFor each user, you can specify a list of optional paths that this user is allowed to\\naccess. A special user anonymous can be used to set the paths for client without an\\nauthorization header. The paths are optional and used for proxy only to authorize.\\n\",\n" +
            "      \"externalized\" : true,\n" +
            "      \"defaultValue\" : \"\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"type\" : \"object\",\n" +
            "  \"description\" : \"Basic Authentication Security Configuration for light-4j\"\n" +
            "}";
    private static final String apiKeyMetadata = "{\n" +
            "  \"properties\" : {\n" +
            "    \"enabled\" : {\n" +
            "      \"type\" : \"boolean\",\n" +
            "      \"$id\" : \"dd62bf5c-c45a-41ac-815a-25cf97415d2b\",\n" +
            "      \"externalizedKeyName\" : \"enabled\",\n" +
            "      \"configFieldName\" : \"enabled\",\n" +
            "      \"description\" : \"Enable ApiKey Authentication Handler, default is false.\",\n" +
            "      \"externalized\" : true,\n" +
            "      \"defaultValue\" : \"true\"\n" +
            "    },\n" +
            "    \"hashEnabled\" : {\n" +
            "      \"type\" : \"boolean\",\n" +
            "      \"$id\" : \"353cb374-4e74-41ef-8016-95562030f751\",\n" +
            "      \"externalizedKeyName\" : \"hashEnabled\",\n" +
            "      \"configFieldName\" : \"hashEnabled\",\n" +
            "      \"description\" : \"If API key hash is enabled. The API key will be hashed with PBKDF2WithHmacSHA1 before it is\\nstored in the config file. It is more secure than put the encrypted key into the config file.\\nThe default value is false. If you want to enable it, you need to use the following repo\\nhttps://github.com/networknt/light-hash command line tool to hash the clear text key.\",\n" +
            "      \"externalized\" : true,\n" +
            "      \"defaultValue\" : \"\"\n" +
            "    },\n" +
            "    \"pathPrefixAuths\" : {\n" +
            "      \"ref\" : {\n" +
            "        \"properties\" : {\n" +
            "          \"pathPrefix\" : {\n" +
            "            \"type\" : \"string\",\n" +
            "            \"$id\" : \"2b2d6a4f-8d27-40ad-b131-8d7d42bdb7f2\",\n" +
            "            \"externalizedKeyName\" : \"\",\n" +
            "            \"configFieldName\" : \"pathPrefix\",\n" +
            "            \"description\" : \"\",\n" +
            "            \"externalized\" : false,\n" +
            "            \"defaultValue\" : \"\",\n" +
            "            \"minLength\" : 0,\n" +
            "            \"maxLength\" : 2147483647,\n" +
            "            \"pattern\" : \"^/.*\",\n" +
            "            \"format\" : \"none\"\n" +
            "          },\n" +
            "          \"headerName\" : {\n" +
            "            \"type\" : \"string\",\n" +
            "            \"$id\" : \"bb8d81d9-9f2a-4d3a-a3ea-064318bc79d0\",\n" +
            "            \"externalizedKeyName\" : \"\",\n" +
            "            \"configFieldName\" : \"headerName\",\n" +
            "            \"description\" : \"\",\n" +
            "            \"externalized\" : false,\n" +
            "            \"defaultValue\" : \"\",\n" +
            "            \"minLength\" : 0,\n" +
            "            \"maxLength\" : 2147483647,\n" +
            "            \"pattern\" : \"^[a-zA-Z0-9-_]*$\",\n" +
            "            \"format\" : \"none\"\n" +
            "          },\n" +
            "          \"apiKey\" : {\n" +
            "            \"type\" : \"string\",\n" +
            "            \"$id\" : \"109fd6cf-8718-4f33-b70e-3f8eb77acc25\",\n" +
            "            \"externalizedKeyName\" : \"\",\n" +
            "            \"configFieldName\" : \"apiKey\",\n" +
            "            \"description\" : \"\",\n" +
            "            \"externalized\" : false,\n" +
            "            \"defaultValue\" : \"\",\n" +
            "            \"minLength\" : 0,\n" +
            "            \"maxLength\" : 2147483647,\n" +
            "            \"pattern\" : \"\",\n" +
            "            \"format\" : \"none\"\n" +
            "          }\n" +
            "        },\n" +
            "        \"type\" : \"object\"\n" +
            "      },\n" +
            "      \"type\" : \"array\",\n" +
            "      \"$id\" : \"368ac7b4-f541-499d-b36b-24f52225b706\",\n" +
            "      \"externalizedKeyName\" : \"pathPrefixAuths\",\n" +
            "      \"configFieldName\" : \"pathPrefixAuths\",\n" +
            "      \"description\" : \"path prefix to the api key mapping. It is a list of map between the path prefix and the api key\\nfor apikey authentication. In the handler, it loops through the list and find the matching path\\nprefix. Once found, it will check if the apikey is equal to allow the access or return an error.\\nThe map object has three properties: pathPrefix, headerName and apiKey. Take a look at the test\\nresources/config folder for configuration examples.\\n\",\n" +
            "      \"externalized\" : true,\n" +
            "      \"minItems\" : 0,\n" +
            "      \"maxItems\" : 2147483647,\n" +
            "      \"uniqueItems\" : false,\n" +
            "      \"contains\" : false,\n" +
            "      \"useSubObjectDefault\" : false,\n" +
            "      \"defaultValue\" : \"\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"type\" : \"object\",\n" +
            "  \"description\" : \"ApiKey Authentication Security Configuration for light-4j\"\n" +
            "}";
    private static final ObjectMapper MAPPER = new ObjectMapper();


    @Test
    public void writeJsonSchemaToFile() throws IOException {
        final var metadata = MAPPER.readValue(apiKeyMetadata, new TypeReference<LinkedHashMap<String, Object>>() {});
        final var generator = new JsonSchemaGenerator("apikey-test", "apikey-test");
        generator.writeSchemaToFile(new FileWriter("./src/test/resources/schemaGenerator/apikey-test.json"),  metadata);
        final var file1 = new File("src/test/resources/schemaGenerator/apikey-test.json");
        final var file2 = new File("src/test/resources/schemaGenerator/apikey-compare.json");
        assertEquals(Files.readString(file1.toPath(), StandardCharsets.UTF_8).replaceAll("\\s", ""), Files.readString(file2.toPath(), StandardCharsets.UTF_8).replaceAll("\\s", ""));
    }

    @Test
    public void testMapFieldJsonSchemaToFile() throws IOException {
        final var metadata = MAPPER.readValue(basicAuthMetadata, new TypeReference<LinkedHashMap<String, Object>>() {});
        final var generator = new JsonSchemaGenerator("basic-auth-test", "basic-auth-test");
        generator.writeSchemaToFile(new FileWriter("./src/test/resources/schemaGenerator/basic-auth-test.json"),  metadata);
        final var file1 = new File("src/test/resources/schemaGenerator/basic-auth-test.json");
        final var file2 = new File("src/test/resources/schemaGenerator/basic-auth-compare.json");
        assertEquals(Files.readString(file1.toPath(), StandardCharsets.UTF_8).replaceAll("\\s", ""), Files.readString(file2.toPath(), StandardCharsets.UTF_8).replaceAll("\\s", ""));
    }

    @Test
    public void testClientConfigYamlSchemaToFile() throws IOException {
        final var metadata = MAPPER.readValue(clientConfigMetadata, new TypeReference<LinkedHashMap<String, Object>>() {});
        final var generator = new YamlGenerator("client-test", "client-test");
        generator.writeSchemaToFile(new FileWriter("./src/test/resources/schemaGenerator/client-test.yaml"),  metadata);
        final var file1 = new File("src/test/resources/schemaGenerator/client-test.yaml");
        final var file2 = new File("src/test/resources/schemaGenerator/client-compare.yaml");
        assertEquals(Files.readString(file1.toPath(), StandardCharsets.UTF_8).replaceAll("\\s", ""), Files.readString(file2.toPath(), StandardCharsets.UTF_8).replaceAll("\\s", ""));
    }

}
