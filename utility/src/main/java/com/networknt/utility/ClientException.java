package com.networknt.utility;

/**
 * Created by steve on 02/09/16.
 */
public class ClientException extends Exception {
    private static final long serialVersionUID = 1L;

    public ClientException() {
        super();
    }

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientException(Throwable cause) {
        super(cause);
    }

}
