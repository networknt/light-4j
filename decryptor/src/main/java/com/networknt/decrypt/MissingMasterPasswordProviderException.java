package com.networknt.decrypt;

/** A provider selection error, distinct from a bad password or malformed ciphertext. */
public final class MissingMasterPasswordProviderException extends IllegalStateException {
    /** Creates an exception with provider-selection guidance and no secret data. */
    public MissingMasterPasswordProviderException() {
        super("No master-password provider configured. Select AutoAESSaltDecryptor "
                + "with an explicit light_4j_config_password, ManualAESSaltDescryptor, or a custom password provider.");
    }
}
