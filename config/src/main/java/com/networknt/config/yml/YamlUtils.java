package com.networknt.config.yml;

import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.resolver.CoreScalarResolver;
import org.snakeyaml.engine.v2.resolver.ScalarResolver;
import org.snakeyaml.engine.v2.schema.CoreSchema;

/** Shared YAML 1.2 Core settings for configuration readers and writers. */
public final class YamlUtils {
    private YamlUtils() {
    }

    public static LoadSettings loadSettings(boolean decrypt) {
        CoreSchema schema = decrypt ? new CoreSchema() {
            @Override
            public ScalarResolver getScalarResolver() {
                CoreScalarResolver resolver = new CoreScalarResolver(true);
                resolver.addImplicitResolver(YmlConstants.CRYPT_TAG, YmlConstants.CRYPT_PATTERN,
                        YmlConstants.CRYPT_FIRST);
                return resolver;
            }
        } : new CoreSchema();
        return LoadSettings.builder().setSchema(schema).build();
    }

    /** Creates a reader that leaves encrypted strings unchanged. Readers are not thread safe. */
    public static Load newLoad() {
        return new Load(loadSettings(false));
    }

    /** Creates a reader using the configured decryptor for unquoted CRYPT scalars. */
    public static Load newLoad(String decryptorClass) {
        if (decryptorClass == null || decryptorClass.isBlank()) {
            return newLoad();
        }
        return new Load(loadSettings(true), DecryptConstructor.getInstance(decryptorClass));
    }

    /** Creates a block-style writer with the same scalar schema as the readers. */
    public static Dump newDump() {
        return new Dump(DumpSettings.builder().setSchema(new CoreSchema())
                .setDefaultFlowStyle(FlowStyle.BLOCK).build());
    }
}
