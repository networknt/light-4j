package com.networknt.zookeeper;

import com.networknt.registry.URL;
import com.networknt.registry.support.command.CommandListener;
import com.networknt.registry.support.command.ServiceListener;
import com.networknt.utility.Constants;
import org.I0Itec.zkclient.ZkClient;
import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * Created by stevehu on 2017-01-11.
 */
public class ZookeeperRegistryTest {
    private ZookeeperRegistry registry;
    private URL serviceUrl, clientUrl;
    private ZkClient zkClient;
    private String service = "com.networknt.light.demoService";
    private TestingServer zookeeper;

    @Before
    public void setUp() throws Exception
    {
        int port = 9000;
        URL zkUrl = new URL("zookeeper", "127.0.0.1", port, "com.networknt.registry.RegistryService");
        clientUrl = new URL(Constants.PROTOCOL_LIGHT, "127.0.0.1", 0, service);
        clientUrl.addParameter("group", "aaa");

        serviceUrl = new URL(Constants.PROTOCOL_LIGHT, "127.0.0.1", 8001, service);
        serviceUrl.addParameter("group", "aaa");

        InstanceSpec spec = new InstanceSpec(null, port, -1, -1, true, 1,-1, -1,new HashMap<String, Object>());
        zookeeper = new TestingServer(spec, true);

        zkClient = new ZkClient("127.0.0.1:" + port, 5000);
        registry = new ZookeeperRegistry(zkUrl, zkClient);
    }

    @After
    public void tearDown() throws Exception
    {
        zookeeper.stop();
    }

    @Test
    public void testVoid() {

    }

    @Test
    public void subAndUnsubService() throws Exception {
        ServiceListener serviceListener = new ServiceListener() {
            @Override
            public void notifyService(URL refUrl, URL registryUrl, List<URL> urls) {
                if (!urls.isEmpty()) {
                    Assert.assertTrue(urls.contains(serviceUrl));
                }
            }
        };
        registry.subscribeService(clientUrl, serviceListener);
        Assert.assertTrue(containsServiceListener(clientUrl, serviceListener));
        registry.doRegister(serviceUrl);
        registry.doAvailable(serviceUrl);
        Thread.sleep(2000);

        registry.unsubscribeService(clientUrl, serviceListener);
        Assert.assertFalse(containsServiceListener(clientUrl, serviceListener));
    }

    private boolean containsServiceListener(URL clientUrl, ServiceListener serviceListener) {
        return registry.getServiceListeners().get(clientUrl).containsKey(serviceListener);
    }

    @Test
    public void subAndUnsubCommand() throws Exception {
        final String command = "{\"index\":0,\"mergeGroups\":[\"aaa:1\",\"bbb:1\"],\"pattern\":\"*\",\"routeRules\":[]}\n";
        CommandListener commandListener = new CommandListener() {
            @Override
            public void notifyCommand(URL refUrl, String commandString) {
                if (commandString != null) {
                    Assert.assertTrue(commandString.equals(command));
                }
            }
        };
        registry.subscribeCommand(clientUrl, commandListener);
        Assert.assertTrue(containsCommandListener(clientUrl, commandListener));

        String commandPath = ZkUtils.toCommandPath(clientUrl);
        if (!zkClient.exists(commandPath)) {
            zkClient.createPersistent(commandPath, true);
        }
        zkClient.writeData(commandPath, command);
        Thread.sleep(2000);

        zkClient.delete(commandPath);

        registry.unsubscribeCommand(clientUrl, commandListener);
        Assert.assertFalse(containsCommandListener(clientUrl, commandListener));
    }

    private boolean containsCommandListener(URL clientUrl, CommandListener commandListener) {
        return registry.getCommandListeners().get(clientUrl).containsKey(commandListener);
    }

    @Test
    public void discoverService() throws Exception {
        registry.doRegister(serviceUrl);
        List<URL> results = registry.discoverService(clientUrl);
        Assert.assertTrue(results.isEmpty());

        registry.doAvailable(serviceUrl);
        results = registry.discoverService(clientUrl);
        Assert.assertTrue(results.contains(serviceUrl));
    }

    @Test
    public void discoverCommand() throws Exception {
        String result = registry.discoverCommand(clientUrl);
        Assert.assertTrue(result.equals(""));

        String command = "{\"index\":0,\"mergeGroups\":[\"aaa:1\",\"bbb:1\"],\"pattern\":\"*\",\"routeRules\":[]}\n";
        String commandPath = ZkUtils.toCommandPath(clientUrl);
        if (!zkClient.exists(commandPath)) {
            zkClient.createPersistent(commandPath, true);
        }
        zkClient.writeData(commandPath, command);
        result = registry.discoverCommand(clientUrl);
        Assert.assertTrue(result.equals(command));
    }

    @Test
    public void doRegisterAndAvailable() throws Exception {
        String node = serviceUrl.getServerPortStr();
        List<String> available, unavailable;
        String unavailablePath = ZkUtils.toNodeTypePath(serviceUrl, ZkNodeType.UNAVAILABLE_SERVER);
        String availablePath = ZkUtils.toNodeTypePath(serviceUrl, ZkNodeType.AVAILABLE_SERVER);

        registry.doRegister(serviceUrl);
        unavailable = zkClient.getChildren(unavailablePath);
        Assert.assertTrue(unavailable.contains(node));

        registry.doAvailable(serviceUrl);
        unavailable = zkClient.getChildren(unavailablePath);
        Assert.assertFalse(unavailable.contains(node));
        available = zkClient.getChildren(availablePath);
        Assert.assertTrue(available.contains(node));

        registry.doUnavailable(serviceUrl);
        unavailable = zkClient.getChildren(unavailablePath);
        Assert.assertTrue(unavailable.contains(node));
        available = zkClient.getChildren(availablePath);
        Assert.assertFalse(available.contains(node));

        registry.doUnregister(serviceUrl);
        unavailable = zkClient.getChildren(unavailablePath);
        Assert.assertFalse(unavailable.contains(node));
        available = zkClient.getChildren(availablePath);
        Assert.assertFalse(available.contains(node));
    }

}
