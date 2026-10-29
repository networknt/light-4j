package com.networknt.decrypt;

import java.util.ServiceConfigurationError;

/** A provider selection error, distinct from a bad password or malformed ciphertext. */
public final class MissingMasterPasswordProviderException extends IllegalStateException {
    private static final String GUIDANCE = "No master-password provider configured. Select AutoAESSaltDecryptor "
            + "with an explicit light_4j_config_password, ManualAESSaltDescryptor, or a custom password provider.";

    /**
     * Creates provider-selection guidance with a standard fatal service-configuration cause.
     * Reflective service factories can honor that signal without depending on this class.
     */
    public MissingMasterPasswordProviderException() {
        super(GUIDANCE, new ServiceConfigurationError(GUIDANCE));
    }
}
