package com.networknt.exception;

import com.networknt.status.Status;

/**
 * Defualt Exception processor for the API exceptions. If user doesn't set the Exception processor in the handler.yml
 * API will use this for exception handler.
 * User defined Exception processor should extends from this default processor
 */
public class DefaultExceptionProcessor {

    static final String STATUS_UNCAUGHT_EXCEPTION = "ERR10011";
    static final String TOKEN_EXPIRED = "ERR10004";

    @ExceptionIndicator(value = FrameworkException.class)
    public Status exception(FrameworkException exception) {
        return exception.getStatus();
    }

    @ExceptionIndicator(value = ApiException.class)
    public Status exception(ApiException exception) {
        return exception.getStatus();
    }

    @ExceptionIndicator(value = ClientException.class)
    public Status exception(ClientException exception) {
        if(exception.getStatus().getStatusCode() == 0){
            return new Status(STATUS_UNCAUGHT_EXCEPTION);
        } else {
            return exception.getStatus();
        }
    }

    @ExceptionIndicator(value = ExpiredTokenException.class)
    public Status exception(ExpiredTokenException exception) {
        Status status = new Status(TOKEN_EXPIRED);
        status.setMessage(exception.getMessage());
        return status;
    }
}
