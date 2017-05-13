package com.networknt.security.outbound;

/**
 * Message Signer
 *
 * @author Steve Hu
 */
public interface MessageSigner {
    SignedMessage sign(Message message);
}
