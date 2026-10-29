package com.networknt.common;

import com.networknt.decrypt.AESSaltDecryptor;
import com.networknt.decrypt.Decryptor;
import com.networknt.decrypt.MissingMasterPasswordProviderException;
import com.networknt.service.SingletonServiceFactory;

import java.util.HashMap;
import java.util.Map;

/** A child JVM isolates failed class initialization and all service/config singletons. */
public final class PasswordProviderServiceProbe {
    private static boolean before;
    private static boolean after;

    public static void main(String[] args) {
        if (args[0].equals("valid")) {
            if (SingletonServiceFactory.getBean(Decryptor.class) == null || !before || !after) {
                throw new AssertionError("Expected the provider and all singleton bindings");
            }
            var map = new HashMap<String, Object>(Map.of("secret",
                    "CRYPT:e6e1b4a099902a2106509e87df909de4:cd9153addc755eb7e00d817c:e499d4501d0372fe37073d88a1025f1970a30cbd9acec7d5"));
            DecryptUtil.decryptMap(map);
            if (!"password".equals(map.get("secret"))) throw new AssertionError("DecryptUtil failed");
            return;
        }
        try {
            SingletonServiceFactory.getBean(Decryptor.class);
        } catch (ExceptionInInitializerError failure) {
            boolean missingProvider = false;
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof MissingMasterPasswordProviderException) missingProvider = true;
            }
            if (!missingProvider || !before || after) throw new AssertionError("Unexpected initialization failure", failure);
            try {
                SingletonServiceFactory.getBean(Runnable.class);
            } catch (NoClassDefFoundError expected) {
                return; // Even an already-created binding cannot escape the failed factory.
            }
            throw new AssertionError("Partially registered singleton escaped failed initialization");
        }
        throw new AssertionError("Missing provider was swallowed by the service factory");
    }

    public static class Before implements Runnable {
        public Before() { before = true; }
        @Override public void run() { }
    }
    public static class After {
        public After() { after = true; }
    }
    public static class ConstructorFallback extends AESSaltDecryptor {
        public ConstructorFallback() { super.getPassword(); }
    }
}
