package com.networknt.db.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of DbProvider interface.
 * It is used when no other implementation is configured in service.yml.
 * It is also used for testing.
 *
 * @author Steve Hu
 */
public class DbProviderImpl implements DbProvider {
    public static final String SQL_EXCEPTION = "ERR10017";
    public static final String GENERIC_EXCEPTION = "ERR10014";
    public static final String OBJECT_NOT_FOUND = "ERR11637";

    private static final Logger logger = LoggerFactory.getLogger(DbProviderImpl.class);


}
