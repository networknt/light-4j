package com.networknt.decrypt;

import java.io.Console;
import java.util.Scanner;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.concurrent.TimeUnit;
import java.security.GeneralSecurityException;
import javax.crypto.spec.SecretKeySpec;

/**
 * This decryptor supports retrieving decrypted password of configuration
 * files from stdin.
 * <p>
 * To use this decryptor, adding the following line into config.yml
 * decryptorClass: com.networknt.decrypt.ManualAESSaltDecryptor
 *
 * The difference between this implementation and the ManualAESDescryptor
 * is that one are using a dynamic salt and the salt will be part of the
 * secret text for strong encryption.
 */
public class ManualAESSaltDescryptor extends AESSaltDecryptor implements AutoCloseable {
    /**
     * Default constructor for ManualAESSaltDescryptor.
     */
    public ManualAESSaltDescryptor() {
        this(32, TimeUnit.MINUTES.toNanos(5), System::nanoTime);
    }

    private final int maximumKeys;
    private final long maximumAgeNanos;
    private final LongSupplier clock;
    private final Map<String, CachedKey> keys = new LinkedHashMap<>();
    private boolean closed;

    // Deterministic capacity/expiration tests without sleeps or real console input.
    ManualAESSaltDescryptor(int maximumKeys, long maximumAgeNanos, LongSupplier clock) {
        if (maximumKeys < 1 || maximumAgeNanos < 1) throw new IllegalArgumentException("Invalid manual key cache limits");
        this.maximumKeys = maximumKeys;
        this.maximumAgeNanos = maximumAgeNanos;
        this.clock = clock;
    }

    @Override
    protected synchronized SecretKeySpec getSecret(byte[] salt) throws GeneralSecurityException {
        if (closed) throw new IllegalStateException("Manual decryptor is closed");
        long now = clock.getAsLong();
        Iterator<CachedKey> iterator = keys.values().iterator();
        while (iterator.hasNext()) {
            CachedKey entry = iterator.next();
            if (now - entry.created >= maximumAgeNanos) {
                Arrays.fill(entry.bytes, (byte) 0);
                iterator.remove();
            }
        }
        String id = HexFormat.of().formatHex(salt);
        CachedKey entry = keys.get(id);
        if (entry != null) return new SecretKeySpec(entry.bytes, "AES");
        return super.getSecret(salt);
    }

    @Override
    protected synchronized void authenticatedKey(byte[] salt, SecretKeySpec secret) {
        if (closed) return; // A close during decryption must not repopulate the cache.
        String id = HexFormat.of().formatHex(salt);
        if (keys.containsKey(id)) return;
        keys.put(id, new CachedKey(secret.getEncoded(), clock.getAsLong()));
        if (keys.size() > maximumKeys) {
            Iterator<CachedKey> oldest = keys.values().iterator();
            Arrays.fill(oldest.next().bytes, (byte) 0);
            oldest.remove();
        }
    }

    private static final class CachedKey {
        private final byte[] bytes;
        private final long created;
        private CachedKey(byte[] bytes, long created) {
            this.bytes = bytes;
            this.created = created;
        }
    }

    /** Clears cached derived keys and prevents further decryption. */
    @Override
    public synchronized void close() {
        for (CachedKey entry : keys.values()) Arrays.fill(entry.bytes, (byte) 0);
        keys.clear();
        closed = true;
    }

    @Override
    protected char[] getPassword() {
        return ownedPassword(readPassword());
    }

    /** Returns a fresh password array owned by this decrypt operation. */
    protected char[] readPassword() {
        char[] password = null;
        Console console = System.console();
        if (console != null) {
            password = console.readPassword("Password for config decryption: ");
        } else {
            // for IDE testing
            System.out.print("Password for config decryption: ");
            Scanner sc = new Scanner(System.in);
            if (sc.hasNext()) {
                password = sc.next().toCharArray();
            }
            // System.in belongs to the application; another salt may require a prompt.
        }
        if (password == null || password.length == 0) {
            throw new RuntimeException("The decrypted password of configuration files should not be empty.");
        }
        return password;
    }

}
