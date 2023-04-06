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
    private static final Logger logger = LoggerFactory.getLogger(DbProviderImpl.class);


}
