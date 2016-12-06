package com.networknt.registry;

import com.networknt.switcher.SwitcherUtil;
import com.networknt.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Set;

/**
 * Created by stevehu on 2016-12-04.
 */
public abstract class AbstractRegistry implements RegistryService {

    static Logger logger = LoggerFactory.getLogger(AbstractRegistry.class);

    private URL registryUrl;

    public AbstractRegistry(URL registryUrl) {
        this.registryUrl = registryUrl;
    }

    @Override
    public void register(String serviceName, URL url) {
        if (url == null) {
            logger.error("Register with malformed param, url is null");
            return;
        }
        logger.info("Url ({}) will register to Registry [{}]", url, registryUrl);
        doRegister(serviceName, url);
        // available if heartbeat switcher already open
        if (SwitcherUtil.isOpen(Constants.REGISTRY_HEARTBEAT_SWITCHER)) {
            available(serviceName, url);
        }
    }

    @Override
    public void unregister(String serviceName, URL url) {
        if (url == null) {
            logger.error("Unregister with malformed param, url is null");
            return;
        }
        logger.info("[{}] Url ({}) will unregister to Registry [{}]", url, registryUrl);
        doUnregister(serviceName, url);
    }


    protected abstract void doRegister(String serviceName, URL url);

    protected abstract void doUnregister(String serviceName, URL url);

}
