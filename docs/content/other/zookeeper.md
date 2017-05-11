---
date: 2017-02-06T21:34:10-05:00
title: Zookeeper
---

A Zookeeper registry implementation that use Zookeeper as registry and discovery
server. It implements both registry and discovery in the same module for
Zookeeper communication. If the API/server is delivered as docker image, another
product called registrator will be used to register it with Zookeeper server.
Otherwise, the server module will be responsible to register itself during
startup.

## Interface

Here is the interface of ZooKeeper client. 

```
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
```

## Implementation

The implementation is based on the zkclient which is an open source library
of ZooKeeper client.

## Configuration

There is no specific config file for ZooKeeper module as it will utilize service.yml

Here is an example of service.yml in test folder for ZooKeeper module to define that 
client is used for ZooKeeperClient interface.

```
description: singleton service factory configuration
singletons:
- com.networknt.registry.URL:
  - com.networknt.registry.URLImpl:
      protocol: zookeeper
      host: 127.0.0.1
      port: 9000
      path: com.networknt.registry.RegistryService
      parameters:
        connectTimeout: '1000'
        registrySessionTimeout: '60000'
- com.networknt.zookeeper.client.ZooKeeperClient:
  - com.networknt.zookeeper.client.ZooKeeperClientImpl:
    - java.lang.String: 127.0.0.1:9000
    - int: 1000
    - int: 60000
- com.networknt.registry.Registry:
  - com.networknt.zookeeper.ZooKeeperRegistry

```
