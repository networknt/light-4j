/*
 * Copyright 2010-2013 Coda Hale and Yammer, Inc., 2014-2017 Dropwizard Team
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

package io.dropwizard.metrics;

import java.io.IOException;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import java.lang.management.ManagementFactory;
import java.util.Set;

/**
 * A {@link Gauge} implementation which queries an {@link MBeanServerConnection} for an attribute of an object.
 */
public class JmxAttributeGauge implements Gauge<Object> {
    private final MBeanServerConnection mBeanServerConn;
    private final ObjectName objectName;
    private final String attributeName;

    /**
     * Creates a new JmxAttributeGauge.
     *
     * @param objectName    the name of the object
     * @param attributeName the name of the object's attribute
     */
    public JmxAttributeGauge(ObjectName objectName, String attributeName) {
        this(ManagementFactory.getPlatformMBeanServer(), objectName, attributeName);
    }

    /**
     * Creates a new JmxAttributeGauge.
     *
     * @param mBeanServerConn  the {@link MBeanServerConnection}
     * @param objectName       the name of the object
     * @param attributeName    the name of the object's attribute
     */
    public JmxAttributeGauge(MBeanServerConnection mBeanServerConn, ObjectName objectName, String attributeName) {
        this.mBeanServerConn = mBeanServerConn;
        this.objectName = objectName;
        this.attributeName = attributeName;
    }

    @Override
    public Object getValue() {
        try {
            return mBeanServerConn.getAttribute(getObjectName(), attributeName);
        } catch (IOException e) {
            return null;
        } catch (JMException e) {
            return null;
        }
    }

    private ObjectName getObjectName() throws IOException {
        if (objectName.isPattern()) {
            Set<ObjectName> foundNames = mBeanServerConn.queryNames(objectName, null);
            if (foundNames.size() == 1) {
                return foundNames.iterator().next();
            }
        }
        return objectName;
    }
}
