package com.networknt.decrypt;

import com.networknt.service.api.FatalServiceConfiguration;

/** A provider selection error, distinct from a bad password or malformed ciphertext. */
public final class MissingMasterPasswordProviderException extends IllegalStateException implements FatalServiceConfiguration {
    private static final String GUIDANCE = "No master-password provider configured. Select AutoAESSaltDecryptor "
            + "with an explicit light_4j_config_password, ManualAESSaltDescryptor, or a custom password provider.";

    /**
     * Creates provider-selection guidance with an explicit fatal-configuration contract.
     * No synthetic cause is attached.
     */
    public MissingMasterPasswordProviderException() {
        super(GUIDANCE);
    }
}
