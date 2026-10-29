package com.networknt.common;

import com.networknt.decrypt.AESSaltDecryptor;

/** Explicit password for synthetic common-module fixtures only. */
public final class FixturePasswordDecryptor extends AESSaltDecryptor {
    @Override protected char[] getPassword() { return ownedPassword("light".toCharArray()); }
}
