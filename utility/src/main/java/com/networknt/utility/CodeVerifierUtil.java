package com.networknt.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Perform code exchange according to Proof Key for Code Exchange (PKCE) spec.
 *
 * @see "Proof Key for Code Exchange by OAuth Public Clients (RFC 7636)
 * <https://tools.ietf.org/html/rfc7636>"
 *
 * @author Steve Hu
 */
public class CodeVerifierUtil {

    static final Logger logger = LoggerFactory.getLogger(CodeVerifierUtil.class);

    /**
     * SHA-256 based code verifier challenge method.
     *
     * @see "Proof Key for Code Exchange by OAuth Public Clients (RFC 7636), Section 4.3
     * <https://tools.ietf.org/html/rfc7636#section-4.3>"
     */
    public static final String CODE_CHALLENGE_METHOD_S256 = "S256";

    /**
     * Plain-text code verifier challenge method. This is only used by AppAuth for Android if
     * SHA-256 is not supported on this platform.
     *
     * @see "Proof Key for Code Exchange by OAuth Public Clients (RFC 7636), Section 4.4
     * <https://tools.ietf.org/html/rfc7636#section-4.4>"
     */
    public static final String CODE_CHALLENGE_METHOD_PLAIN = "plain";

    /**
     * The minimum permitted length for a code verifier.
     *
     * @see "Proof Key for Code Exchange by OAuth Public Clients (RFC 7636), Section 4.1
     * <https://tools.ietf.org/html/rfc7636#section-4.1>"
     */
    public static final int MIN_CODE_VERIFIER_LENGTH = 43;

    /**
     * The maximum permitted length for a code verifier.
     *
     * @see "Proof Key for Code Exchange by OAuth Public Clients (RFC 7636), Section 4.1
     * <https://tools.ietf.org/html/rfc7636#section-4.1>"
     */
    public static final int MAX_CODE_VERIFIER_LENGTH = 128;

    /**
     * The default entropy (in bytes) used for the code verifier.
     */
    public static final int DEFAULT_CODE_VERIFIER_ENTROPY = 64;

    /**
     * The minimum permitted entropy (in bytes) for use with
     * {@link #generateRandomCodeVerifier(SecureRandom,int)}.
     */
    public static final int MIN_CODE_VERIFIER_ENTROPY = 32;

    /**
     * The maximum permitted entropy (in bytes) for use with
     * {@link #generateRandomCodeVerifier(SecureRandom,int)}.
     */
    public static final int MAX_CODE_VERIFIER_ENTROPY = 96;

    /**
     * Regex for legal code verifier strings, as defined in the spec.
     *
     * @see "Proof Key for Code Exchange by OAuth Public Clients (RFC 7636), Section 4.1
     * <https://tools.ietf.org/html/rfc7636#section-4.1>"
     */
    public static final Pattern VALID_CODE_CHALLENGE_PATTERN = Pattern.compile("^[0-9a-zA-Z\\-\\.~_]+$");


    /**
     * Generates a random code verifier string using {@link SecureRandom} as the source of
     * entropy, with the default entropy quantity as defined by
     * {@link #DEFAULT_CODE_VERIFIER_ENTROPY}.
     */
    public static String generateRandomCodeVerifier() {
        return generateRandomCodeVerifier(new SecureRandom(), DEFAULT_CODE_VERIFIER_ENTROPY);
    }

    /**
     * Generates a random code verifier string using the provided entropy source and the specified
     * number of bytes of entropy.
     */
    public static String generateRandomCodeVerifier(SecureRandom entropySource, int entropyBytes) {
        byte[] randomBytes = new byte[entropyBytes];
        entropySource.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Produces a challenge from a code verifier, using SHA-256 as the challenge method if the
     * system supports it (all Android devices _should_ support SHA-256), and falls back
     * to the "plain" challenge type if unavailable.
     */
    public static String deriveCodeVerifierChallenge(String codeVerifier) {
        try {
            MessageDigest sha256Digester = MessageDigest.getInstance("SHA-256");
            sha256Digester.update(codeVerifier.getBytes("ISO_8859_1"));
            byte[] digestBytes = sha256Digester.digest();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digestBytes);
        } catch (NoSuchAlgorithmException e) {
            logger.warn("SHA-256 is not supported on this device! Using plain challenge", e);
            return codeVerifier;
        } catch (UnsupportedEncodingException e) {
            logger.error("ISO-8859-1 encoding not supported on this device!", e);
            throw new IllegalStateException("ISO-8859-1 encoding not supported", e);
        }
    }

    /**
     * Returns the challenge method utilized on this system: typically SHA-256 if supported by
     * the system, plain otherwise.
     */
    public static String getCodeVerifierChallengeMethod() {
        try {
            MessageDigest.getInstance("SHA-256");
            // no exception, so SHA-256 is supported
            return CODE_CHALLENGE_METHOD_S256;
        } catch (NoSuchAlgorithmException e) {
            return CODE_CHALLENGE_METHOD_PLAIN;
        }
    }

}
