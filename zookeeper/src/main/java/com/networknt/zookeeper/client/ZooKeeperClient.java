/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
