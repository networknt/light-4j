package com.networknt.utility;

/**
 * Created by steve on 02/09/16.
 */
public class ApiException extends Exception {
    private static final long serialVersionUID = 1L;
    private ErrorResponse errorResponse;

    public ErrorResponse getErrorResponse() {
        return errorResponse;
    }

    public ApiException(ErrorResponse errorResponse) {
        this.errorResponse = errorResponse;
    }

    public ApiException(int code, String message) {
        super(message);
        this.errorResponse = new ErrorResponse(code, message);
    }

    public ApiException(int code, String message, Throwable cause) {
        super(message, cause);
        this.errorResponse = new ErrorResponse(code, message);
    }

    public ApiException(ErrorResponse errorResponse, Throwable cause) {
        super(cause);
        this.errorResponse = errorResponse; }
}
