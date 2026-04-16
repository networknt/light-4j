package com.networknt.token;

/**
 * TokenClient defines the contract for securely exchanging cleartext PII for format-preserved proxy tokens.
 * It is meant to be implemented via a multi-level cache Decorator pattern (e.g. L1 -> L2 -> L3 HTTP).
 */
public interface TokenClient {

    /**
     * Tokenizes a value using the specified scheme.
     * @param value The cleartext value.
     * @param schemeId The format scheme identifier mapping to the Tokenization service DB.
     * @return The tokenized proxy string.
     */
    String tokenize(String value, int schemeId);

    /**
     * Detokenizes a proxy token back to its cleartext value.
     * @param token The proxy string.
     * @return The cleartext value.
     */
    String detokenize(String token);
}
