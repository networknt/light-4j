package com.networknt.utility;

public interface Decryptor {

    String CRYPT_PREFIX = "CRYPT";

    /**
     * This is the method that decrypt an encrypted value. The logic of
     * encryption and decryption are free to be implemented by the customer.
     *
     * A default implementation is included but customers should have done
     * their own implementation so this is an open source project. Also, if
     * there are enough demand, we might be able to provide encryption as
     * service from encrypt.networknt.com and provide decryption jar file per
     * customer with is more secure.
     *
     * @param input encrypted string
     * @return decrypted string
     */
    String decrypt(String input);

}
