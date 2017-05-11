package com.networknt.zookeeper.client;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.exception.ZkException;
import org.I0Itec.zkclient.exception.ZkInterruptedException;

import java.util.List;

/**
 * ZooKeeperClient interface
 *
 * @author Steve Hu
 */
public interface ZooKeeperClient {

    void subscribeStateChanges(IZkStateListener listener);

    java.util.List<String> subscribeChildChanges(String path, IZkChildListener listener);

    void unsubscribeChildChanges(String path, IZkChildListener childListener);

    void subscribeDataChanges(String path, IZkDataListener listener);

    void unsubscribeDataChanges(String path, IZkDataListener dataListener);

    boolean exists(String path);

    List<String> getChildren(String path);

    @SuppressWarnings("TypeParameterUnusedInFormals")
    <T> T readData(String path);

    @SuppressWarnings("TypeParameterUnusedInFormals")
    <T> T readData(String path, boolean returnNullIfPathNotExists);

    void writeData(String path, Object object);

    void createPersistent(String path, boolean createParents) throws ZkInterruptedException, IllegalArgumentException, ZkException, RuntimeException;

    void createEphemeral(String path, Object data) throws ZkInterruptedException, IllegalArgumentException, ZkException, RuntimeException;

    boolean delete(String path);

}
