package com.networknt.decrypt;

import java.io.Console;
import java.util.Scanner;

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
public class ManualAESSaltDescryptor extends AESSaltDecryptor {
    @Override
    protected char[] getPassword() {
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
            sc.close();
        }
        if (password == null || password.length == 0) {
            throw new RuntimeException("The decrypted password of configuration files should not be empty.");
        }
        return password;
    }

}
