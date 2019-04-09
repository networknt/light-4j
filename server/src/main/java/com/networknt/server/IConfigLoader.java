package com.networknt.server;

/**
 * Config Loader to fetch and load configs
 *
 * To use this Config Loader, please implement it and add implementation class to startup.yml as configLoaderClass: com.networknt.server.DefaultConfigLoader
 * so that Server class can find, instantiate and the trigger its init() method.
 */
public interface IConfigLoader {
    void init();
}
