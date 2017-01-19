package com.networknt.zookeeper.client;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkException;
import org.I0Itec.zkclient.exception.ZkInterruptedException;

import java.util.List;

/**
 * Created by stevehu on 2017-01-18.
 */
public class ZooKeeperClientImpl implements ZooKeeperClient {

    private ZkClient zkClient;

    private String zkServers;
    private int sessionTimeout;
    private int connectionTimeout;

    public ZooKeeperClientImpl(String zkServers, int sessionTimeout, int connectionTimeout) {
        zkClient = new ZkClient(zkServers, sessionTimeout, connectionTimeout);
    }

    public String getZkServers() {
        return zkServers;
    }

    public void setZkServers(String zkServers) {
        this.zkServers = zkServers;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    @Override
    public void subscribeStateChanges(IZkStateListener listener) {
        zkClient.subscribeStateChanges(listener);
    }

    @Override
    public List<String> subscribeChildChanges(String path, IZkChildListener listener) {
        return zkClient.subscribeChildChanges(path, listener);
    }

    @Override
    public void unsubscribeChildChanges(String path, IZkChildListener childListener) {
        zkClient.unsubscribeChildChanges(path, childListener);
    }

    @Override
    public void subscribeDataChanges(String path, IZkDataListener listener) {
        zkClient.subscribeDataChanges(path, listener);
    }

    @Override
    public void unsubscribeDataChanges(String path, IZkDataListener dataListener) {
        zkClient.unsubscribeDataChanges(path, dataListener);
    }

    @Override
    public boolean exists(String path) {
        return zkClient.exists(path);
    }

    @Override
    public List<String> getChildren(String path) {
        return zkClient.getChildren(path);
    }

    @Override
    public <T> T readData(String path) {
        return zkClient.readData(path);
    }

    @Override
    public <T> T readData(String path, boolean returnNullIfPathNotExists) {
        return zkClient.readData(path, returnNullIfPathNotExists);
    }

    @Override
    public void writeData(String path, Object object) {
        zkClient.writeData(path, object);
    }

    @Override
    public void createPersistent(String path, boolean createParents) throws ZkInterruptedException, IllegalArgumentException, ZkException, RuntimeException {
        zkClient.createPersistent(path, createParents);
    }

    @Override
    public void createEphemeral(String path, Object data) throws ZkInterruptedException, IllegalArgumentException, ZkException, RuntimeException {
        zkClient.createEphemeral(path, data);
    }

    @Override
    public boolean delete(String path) {
        return zkClient.delete(path);
    }
}
