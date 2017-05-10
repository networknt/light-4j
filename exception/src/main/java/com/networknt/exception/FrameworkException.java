package com.networknt.exception;

import com.networknt.status.Status;

/**
 * This is similar exception like ApiException but it is RuntimeException.
 * It should be used not against consumer request but only internally in
 * the framework. For example, check if the passed in parameter is null etc.
 *
 * The FrameworkException will be handled by ExceptionHandler before the
 * exchange ends and a meaningful status will be returned to the consumer.
 *
 * @author Steve Hu
 */
public class FrameworkException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final Status status;

    public Status getStatus() {
        return status;
    }

    public FrameworkException(Status status) {
        super(status.toString());
        this.status = status;
    }

    public FrameworkException(Status status, Throwable cause) {
        super(status.toString(), cause);
        this.status = status;
    }

}
