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

import com.networknt.exception.FrameworkException;
import com.networknt.registry.NotifyListener;
import com.networknt.registry.URL;
import com.networknt.registry.URLImpl;
import com.networknt.registry.URLParamType;
import com.networknt.status.Status;
import com.networknt.switcher.SwitcherUtil;
import com.networknt.utility.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class CommandServiceManager implements ServiceListener {
    private static final Logger logger = LoggerFactory.getLogger(CommandServiceManager.class);
    private static final String REGISTRY_IS_NULL = "ERR10024";
    private static final String WEIGHT_OUT_OF_RANGE = "ERR10025";

    public static final String LIGHT_COMMAND_SWITCHER = "feature.light.command.enable";
    //private static Pattern IP_PATTERN = Pattern.compile("^!?[0-9.]*\\*?$");

    static {
        SwitcherUtil.initSwitcher(LIGHT_COMMAND_SWITCHER, true);
    }

    private URL refUrl;
    private ConcurrentHashSet<NotifyListener> notifySet;
    private CommandFailbackRegistry registry;
    // service cache
    private Map<String, List<URL>> groupServiceCache;
    // command cache
    //private String commandStringCache = "";
    //private volatile RpcCommand commandCache;

    public CommandServiceManager(URL refUrl) {
        if(logger.isInfoEnabled()) logger.info("CommandServiceManager init url:" + refUrl.toFullStr());
        this.refUrl = refUrl;
        notifySet = new ConcurrentHashSet<NotifyListener>();
        groupServiceCache = new ConcurrentHashMap<String, List<URL>>();

    }

    @Override
    public void notifyService(URL serviceUrl, URL registryUrl, List<URL> urls) {

        if (registry == null) {
            throw new FrameworkException(new Status(REGISTRY_IS_NULL));
        }

        URL urlCopy = serviceUrl.createCopy();
        String groupName = urlCopy.getParameter(URLParamType.group.getName(), URLParamType.group.getValue());
        groupServiceCache.put(groupName, urls);

        List<URL> finalResult = new ArrayList<URL>();
        if(logger.isInfoEnabled()) logger.info("command cache is null. service:" + serviceUrl.toSimpleString());
        // if no command cache, return group
        finalResult.addAll(discoverOneGroup(refUrl));

        for (NotifyListener notifyListener : notifySet) {
            notifyListener.notify(registry.getUrl(), finalResult);
        }

    }

    /*
    @Override
    public void notifyCommand(URL serviceUrl, String commandString) {
        if(logger.isInfoEnabled()) logger.info("CommandServiceManager notify command. service:" + serviceUrl.toSimpleString() + ", command:" + commandString);

        if (!SwitcherUtil.isOpen(LIGHT_COMMAND_SWITCHER) || commandString == null) {
            if(logger.isInfoEnabled()) logger.info("command reset empty since swither is close.");
            commandString = "";
        }

        List<URL> finalResult = new ArrayList<URL>();
        URL urlCopy = serviceUrl.createCopy();

        if (!StringUtils.equals(commandString, commandStringCache)) {
            commandStringCache = commandString;
            commandCache = RpcCommandUtil.stringToCommand(commandStringCache);
            Map<String, Integer> weights = new HashMap<String, Integer>();

            if (commandCache != null) {
                commandCache.sort();
                finalResult = discoverServiceWithCommand(refUrl, weights, commandCache);
            } else {
                // If command is abnormal, handle it just like there is no command to prevent wrong command
                if (StringUtils.isNotBlank(commandString)) {
                    logger.warn("command parse fail, ignored! command:" + commandString);
                    commandString = "";
                }
                // If there is no command, return manager group
                finalResult.addAll(discoverOneGroup(refUrl));

            }

            // when command is changed, delete cache and unsubscribe group
            Set<String> groupKeys = groupServiceCache.keySet();
            for (String gk : groupKeys) {
                if (!weights.containsKey(gk)) {
                    groupServiceCache.remove(gk);
                    URL urlTemp = urlCopy.createCopy();
                    urlTemp.addParameter(URLParamType.group.getName(), gk);
                    registry.unsubscribeService(urlTemp, this);
                }
            }
        } else {
            if(logger.isInfoEnabled()) logger.info("command is not changed. url:" + serviceUrl.toSimpleString());
            // command not changed do nothing
            return;
        }

        for (NotifyListener notifyListener : notifySet) {
            notifyListener.notify(registry.getUrl(), finalResult);
        }

        // when command is empty, trigger resub service
        if ("".equals(commandString)) {
            if(logger.isInfoEnabled()) logger.info("reSub service" + refUrl.toSimpleString());
            registry.subscribeService(refUrl, this);
        }
    }

    public List<URL> discoverServiceWithCommand(URL serviceUrl, Map<String, Integer> weights, RpcCommand rpcCommand) {
        String localIP = NetUtils.getLocalAddress().getHostAddress();
        return this.discoverServiceWithCommand(serviceUrl, weights, rpcCommand, localIP);
    }

    public List<URL> discoverServiceWithCommand(URL serviceUrl, Map<String, Integer> weights, RpcCommand rpcCommand, String localIP) {
        if (rpcCommand == null || CollectionUtil.isEmpty(rpcCommand.getClientCommandList())) {
            return discoverOneGroup(serviceUrl);
        }

        List<URL> mergedResult = new LinkedList<URL>();
        String path = serviceUrl.getPath();

        List<RpcCommand.ClientCommand> clientCommandList = rpcCommand.getClientCommandList();
        boolean hit = false;
        for (RpcCommand.ClientCommand command : clientCommandList) {
            mergedResult = new LinkedList<URL>();
            // check if current url matches
            boolean match = RpcCommandUtil.match(command.getPattern(), path);
            if (match) {
                hit = true;
                if (!CollectionUtil.isEmpty(command.getMergeGroups())) {
                    // calculate weight
                    try {
                        buildWeightsMap(weights, command);
                    } catch (FrameworkException e) {
                        logger.error("build weights map fail!" + e.getMessage());
                        continue;
                    }
                    // According to the result, discover each group's services and combine them together
                    mergedResult.addAll(mergeResult(serviceUrl, weights));
                } else {
                    mergedResult.addAll(discoverOneGroup(serviceUrl));
                }

                if(logger.isInfoEnabled()) logger.info("mergedResult: size-" + mergedResult.size() + " --- " + mergedResult.toString());

                if (!CollectionUtil.isEmpty(command.getRouteRules())) {
                    if(logger.isInfoEnabled()) logger.info("router: " + command.getRouteRules().toString());

                    for (String routeRule : command.getRouteRules()) {
                        String[] fromTo = routeRule.replaceAll("\\s+", "").split("to");

                        if (fromTo.length != 2) {
                            logger.warn("Invalid route rule configuration");
                            continue;
                        }
                        String from = fromTo[0];
                        String to = fromTo[1];
                        if (from.length() < 1 || to.length() < 1 || !IP_PATTERN.matcher(from).find() || !IP_PATTERN.matcher(to).find()) {
                            logger.warn("Invalid route rule configuration");
                            continue;
                        }
                        boolean oppositeFrom = from.startsWith("!");
                        boolean oppositeTo = to.startsWith("!");
                        if (oppositeFrom) {
                            from = from.substring(1);
                        }
                        if (oppositeTo) {
                            to = to.substring(1);
                        }
                        int idx = from.indexOf('*');
                        boolean matchFrom;
                        if (idx != -1) {
                            matchFrom = localIP.startsWith(from.substring(0, idx));
                        } else {
                            matchFrom = localIP.equals(from);
                        }

                        // prefixed with !，reverse
                        if (oppositeFrom) {
                            matchFrom = !matchFrom;
                        }
                        if(logger.isInfoEnabled()) logger.info("matchFrom: " + matchFrom + ", localip:" + localIP + ", from:" + from);
                        if (matchFrom) {
                            boolean matchTo;
                            Iterator<URL> iterator = mergedResult.iterator();
                            while (iterator.hasNext()) {
                                URL url = iterator.next();
                                if (url.getProtocol().equalsIgnoreCase("rule")) {
                                    continue;
                                }
                                idx = to.indexOf('*');
                                if (idx != -1) {
                                    matchTo = url.getHost().startsWith(to.substring(0, idx));
                                } else {
                                    matchTo = url.getHost().equals(to);
                                }
                                if (oppositeTo) {
                                    matchTo = !matchTo;
                                }
                                if (!matchTo) {
                                    iterator.remove();
                                    if(logger.isInfoEnabled()) logger.info("router To not match. url remove : " + url.toSimpleString());
                                }
                            }
                        }
                    }
                }
                // use the first one matched TODO Consider if this meet most user cases
                break;
            }
        }

        List<URL> finalResult = new ArrayList<URL>();
        if (!hit) {
            finalResult = discoverOneGroup(serviceUrl);
        } else {
            finalResult.addAll(mergedResult);
        }
        return finalResult;
    }
    */

    private void buildWeightsMap(Map<String, Integer> weights, RpcCommand.ClientCommand command) {
        for (String rule : command.getMergeGroups()) {
            String[] gw = rule.split(":");
            int weight = 1;
            if (gw.length > 1) {
                try {
                    weight = Integer.parseInt(gw[1]);
                } catch (NumberFormatException e) {
                    throw new FrameworkException(new Status(WEIGHT_OUT_OF_RANGE, weight));
                }
                if (weight < 0 || weight > 100) {
                    throw new FrameworkException(new Status(WEIGHT_OUT_OF_RANGE, weight));
                }
            }
            weights.put(gw[0], weight);
        }
    }

    private List<URL> mergeResult(URL url, Map<String, Integer> weights) {
        List<URL> finalResult = new ArrayList<URL>();

        if (weights.size() > 1) {
            // construct a rule url with all groups and added as first
            URL ruleUrl = new URLImpl("rule", url.getHost(), url.getPort(), url.getPath());
            StringBuilder weightsBuilder = new StringBuilder(64);
            for (Map.Entry<String, Integer> entry : weights.entrySet()) {
                weightsBuilder.append(entry.getKey()).append(':').append(entry.getValue()).append(',');
            }
            ruleUrl.addParameter(URLParamType.weights.getName(), weightsBuilder.deleteCharAt(weightsBuilder.length() - 1).toString());
            finalResult.add(ruleUrl);
        }

        for (String key : weights.keySet()) {
            if (groupServiceCache.containsKey(key)) {
                finalResult.addAll(groupServiceCache.get(key));
            } else {
                URL urlTemp = url.createCopy();
                urlTemp.addParameter(URLParamType.group.getName(), key);
                finalResult.addAll(discoverOneGroup(urlTemp));
                registry.subscribeService(urlTemp, this);
            }
        }
        return finalResult;
    }

    private List<URL> discoverOneGroup(URL urlCopy) {
        if(logger.isInfoEnabled()) logger.info("CommandServiceManager discover one group. url:" + urlCopy.toSimpleString());
        String group = urlCopy.getParameter(URLParamType.group.getName(), URLParamType.group.getValue());
        List<URL> list = groupServiceCache.get(group);
        if (list == null) {
            list = registry.discoverService(urlCopy);
            groupServiceCache.put(group, list);
        }
        return list;
    }
    /*
    public void setCommandCache(String command) {
        commandStringCache = command;
        commandCache = RpcCommandUtil.stringToCommand(commandStringCache);
        if(logger.isInfoEnabled()) logger.info("CommandServiceManager set commandcache. commandstring:" + commandStringCache + ", comandcache "
                + (commandCache == null ? "is null." : "is not null."));
    }
    */
    public void addNotifyListener(NotifyListener notifyListener) {
        notifySet.add(notifyListener);
    }

    public void removeNotifyListener(NotifyListener notifyListener) {
        notifySet.remove(notifyListener);
    }

    public void setRegistry(CommandFailbackRegistry registry) {
        this.registry = registry;
    }

}
