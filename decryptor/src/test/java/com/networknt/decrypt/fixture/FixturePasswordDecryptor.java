package com.networknt.decrypt.fixture;

import com.networknt.decrypt.AESSaltDecryptor;

/** Explicit public password for synthetic fixtures only; compiled only into test output. */
public class FixturePasswordDecryptor extends AESSaltDecryptor {
    @Override
    protected char[] getPassword() { return ownedPassword("light".toCharArray()); }
}
