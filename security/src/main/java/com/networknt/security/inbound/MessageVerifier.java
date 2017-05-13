package com.networknt.security.inbound;

import com.networknt.security.outbound.SignedMessage;

/**
 * Message Verifier interface
 *
 * @author Steve Hu
 */
public interface MessageVerifier {
    VerifiedMessage verify(SignedMessage message);
}
