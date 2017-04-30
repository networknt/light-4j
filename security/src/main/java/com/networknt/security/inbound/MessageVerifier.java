package com.networknt.security.inbound;

import com.networknt.security.outbound.SignedMessage;

/**
 * Created by steve on 10/04/17.
 */
public interface MessageVerifier {
    VerifiedMessage verify(SignedMessage message);
}
