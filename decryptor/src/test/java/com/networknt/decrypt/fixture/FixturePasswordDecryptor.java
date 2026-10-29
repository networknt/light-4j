package com.networknt.decrypt.fixture;

import com.networknt.decrypt.AESSaltDecryptor;

/** Explicit public password for synthetic fixtures only; packaged only in the test artifact. */
public class FixturePasswordDecryptor extends AESSaltDecryptor {
    @Override
    protected char[] getPassword() { return ownedPassword("light".toCharArray()); }
}
