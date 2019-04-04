/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.decrypt;

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
