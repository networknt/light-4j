package com.networknt.config;

import com.networknt.decrypt.MissingMasterPasswordProviderException;

/** Child-JVM entry point: exercises only the public configuration API. */
public final class PasswordProviderConfigProbe {
    public static void main(String[] args) {
        if (args[0].equals("valid")) {
            Object password = Config.getInstance().getJsonMapConfig("provider-secret").get("password");
            if (!"password".equals(password)) throw new AssertionError("Explicit provider failed");
            return;
        }
        try {
            Config.getInstance();
        } catch (ExceptionInInitializerError failure) {
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof MissingMasterPasswordProviderException) return;
            }
            throw failure;
        }
        throw new AssertionError("Bare provider must prevent Config initialization before reading CRYPT");
    }
}
