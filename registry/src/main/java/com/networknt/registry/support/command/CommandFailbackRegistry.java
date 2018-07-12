/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.networknt.registry.support.command;

import com.networknt.registry.NotifyListener;
import com.networknt.registry.URL;
import com.networknt.registry.support.FailbackRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CommandFailbackRegistry extends FailbackRegistry {
    private static final Logger logger = LoggerFactory.getLogger(CommandFailbackRegistry.class);
    private ConcurrentHashMap<URL, CommandServiceManager> commandManagerMap;

    public CommandFailbackRegistry(URL url) {
        super(url);
        commandManagerMap = new ConcurrentHashMap<>();
        if(logger.isInfoEnabled()) logger.info("CommandFailbackRegistry init. url: " + url.toSimpleString());
    }

    @Override
    protected void doSubscribe(URL url, final NotifyListener listener) {
        if(logger.isInfoEnabled()) logger.info("CommandFailbackRegistry subscribe. url: " + url.toSimpleString());
        URL urlCopy = url.createCopy();
        CommandServiceManager manager = getCommandServiceManager(urlCopy);
        manager.addNotifyListener(listener);

        subscribeService(urlCopy, manager);
        //subscribeCommand(urlCopy, manager);

        List<URL> urls = doDiscover(urlCopy);
        if (urls != null && urls.size() > 0) {
            this.notify(urlCopy, listener, urls);
        }
    }

    @Override
    protected void doUnsubscribe(URL url, NotifyListener listener) {
        if(logger.isInfoEnabled()) logger.info("CommandFailbackRegistry unsubscribe. url: " + url.toSimpleString());
        URL urlCopy = url.createCopy();
        CommandServiceManager manager = commandManagerMap.get(urlCopy);

        manager.removeNotifyListener(listener);
        unsubscribeService(urlCopy, manager);
        //unsubscribeCommand(urlCopy, manager);

    }

    @Override
    protected List<URL> doDiscover(URL url) {
        if(logger.isInfoEnabled()) logger.info("CommandFailbackRegistry discover. url: " + url.toSimpleString());
        List<URL> finalResult = discoverService(url.createCopy());
        if(logger.isInfoEnabled()) logger.info("CommandFailbackRegistry discover size: " +
                (finalResult==null ? 0 : finalResult.size()) + ", result:" + (finalResult==null ? null: finalResult.toString()));
        return finalResult;
    }

    private CommandServiceManager getCommandServiceManager(URL urlCopy) {
        CommandServiceManager manager = commandManagerMap.get(urlCopy);
        if (manager == null) {
            manager = new CommandServiceManager(urlCopy);
            manager.setRegistry(this);
            CommandServiceManager manager1 = commandManagerMap.putIfAbsent(urlCopy, manager);
            if (manager1 != null) manager = manager1;
        }
        return manager;
    }

    // for UnitTest
    public ConcurrentHashMap<URL, CommandServiceManager> getCommandManagerMap() {
        return commandManagerMap;
    }

    protected abstract void subscribeService(URL url, ServiceListener listener);

    protected abstract void unsubscribeService(URL url, ServiceListener listener);

    protected abstract List<URL> discoverService(URL url);

}
