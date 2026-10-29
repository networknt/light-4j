package com.networknt.service.api;

/**
 * Explicit opt-in contract for an unrecoverable service-provider configuration failure.
 * Implement this on a throwable when reflective factories must abort initialization
 * rather than retain their legacy handling of ordinary construction failures.
 */
public interface FatalServiceConfiguration {
}
