package com.networknt.decrypt;

import java.io.*;
import java.util.Scanner;

/**
 * This decryptor supports retrieving decrypted password of configuration
 * files from stdin.
 * <p>
 * To use this decryptor, adding the following line into config.yml
 * decryptorClass: com.networknt.decrypt.ManualAESDecryptor
 */
public class ManualAESDecryptor extends AESDecryptor {
    @Override
    protected void init() {
        Console console = System.console();
        if (console != null) {
            PASSWORD = console.readPassword("Password for config decryption: ");
        } else {
            // for IDE testing
            System.out.print("Password for config decryption: ");
            Scanner sc = new Scanner(System.in);
            PASSWORD = sc.next().toCharArray();
            sc.close();
        }
        if (PASSWORD == null || PASSWORD.length == 0) {
            throw new RuntimeException("The decrypted password of configuration files should not be empty.");
        }
    }
}
