package com.networknt.registry;

import com.networknt.switcher.SwitcherUtil;
import com.networknt.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * Created by stevehu on 2016-12-04.
 */
public abstract class AbstractRegistry implements RegistryService {

    static final Logger logger = LoggerFactory.getLogger(AbstractRegistry.class);

    private final URL registryUrl;

    public AbstractRegistry(String serviceName, URL registryUrl) {
        // where to get serviceName and registryUrl? from config file which one?

        this.registryUrl = registryUrl;
        SwitcherUtil.registerSwitcherListener(Constants.REGISTRY_HEARTBEAT_SWITCHER, (key, value) -> {
            if (key != null && value != null) {
                if (value) {
                    available(serviceName, null);
                } else {
                    unavailable(serviceName, null);
                }
            }
        });
    }

    @Override
    public void register(String serviceName, URL url) {
        if (url == null) {
            logger.error("Register with malformed param, url is null");
            return;
        }
        logger.info("Service Name [{}] Url ({}) will register to Registry [{}]", serviceName, url, registryUrl);
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
        logger.info("Service Name [{}] Url ({}) will unregister to Registry [{}]", serviceName, url, registryUrl);
        doUnregister(serviceName, url);
    }

    @Override
    public void available(String serviceName, URL url) {
        logger.info("Service Name [{}] Url ({}) will set to available to Registry [{}]", serviceName, url, registryUrl);
        if (url != null) {
            doAvailable(serviceName, url);
        } else {
            doAvailable(serviceName, null);
        }
    }

    @Override
    public void unavailable(String serviceName, URL url) {
        logger.info("Service Name [{}] Url ({}) will set to unavailable to Registry [{}]", serviceName, url, registryUrl);
        if (url != null) {
            doUnavailable(serviceName, url);
        } else {
            doUnavailable(serviceName, null);
        }
    }


    protected abstract void doRegister(String serviceName, URL url);

    protected abstract void doUnregister(String serviceName, URL url);

    protected abstract void doAvailable(String serviceName, URL url);

    protected abstract void doUnavailable(String serviceName, URL url);

}
