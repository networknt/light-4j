package com.networknt.server;

/**
 * Config Loader to fetch and load configs
 *
 * To use this Config Loader, please implement it and add implementation class to startup.yml as configLoaderClass: com.abc.server.ABCConfigLoader
 * so that Server class can find, instantiate and then trigger its init() method.
 */
public interface IConfigLoader {
    void init();

    /**
     * Refresh config values from config server if the config server url is defined
     *
     */
    void reloadConfig();
}
