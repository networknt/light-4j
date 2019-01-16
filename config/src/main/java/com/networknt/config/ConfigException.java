package com.networknt.config;

/**
 * A runtime exception to indicate something is wrong in the configuration
 * file. Most config files will be loaded during server startup in a static
 * block and there is no way an exception can be thrown. To use a runtime
 * exception, we can force the server startup to failed so that some action
 * can be taken by the operation team. If the config file is laze loaded in
 * the application during request handling phase, then this exception will
 * cause a 500 error and most likely be handled by the exception handler in
 * the middleware handler chain.
 *
 * This is a special exception that need to be monitored in logs in order to
 * capture config issue during development phase.
 *
 */
public class ConfigException extends RuntimeException {
    public ConfigException(String message) {
        super(message);
    }
}
