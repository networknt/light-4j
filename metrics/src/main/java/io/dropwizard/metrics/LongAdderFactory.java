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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

final class LongAdderFactory {

    private static final Logger LOG = LoggerFactory.getLogger(LongAdderFactory.class);

    private static final String LONG_ADDER_CLASS_NAME = "java.util.concurrent.atomic.LongAdder";
    private static final String JAVA8_LONG_ADDER_CLASS_NAME = LongAdderFactory.class.getPackage().getName() + ".Java8LongAdderImpl";
    private static final String UNSAFE_LONG_ADDER_CLASS_NAME = LongAdderFactory.class.getPackage().getName() + ".UnsafeLongAdderImpl";

    private static final boolean JAVA8 = isClassLoaded(LONG_ADDER_CLASS_NAME, false) && isClassLoaded(JAVA8_LONG_ADDER_CLASS_NAME, true);
    private static final boolean UNSAFE = isClassLoaded(UNSAFE_LONG_ADDER_CLASS_NAME, true);

    private static final Constructor<LongAdder> JAVA8_LONG_ADDER_CONSTRUCTOR = JAVA8 ? getJava8LongAdderConstructor() : null;

    static LongAdder create() {
        if (JAVA8 && JAVA8_LONG_ADDER_CONSTRUCTOR != null) {
            try {
                return JAVA8_LONG_ADDER_CONSTRUCTOR.newInstance();
            } catch (IllegalArgumentException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                LOG.info("Error instantiating Java8LongAdderImpl", e);
            }
        }

        if (UNSAFE) {
            return new UnsafeLongAdderImpl();
        }

        return new LongAdderImpl();
    }

    private static boolean isClassLoaded(String className, boolean canInitialize) {
        Class<?> clazz;
        try {
            clazz = LongAdderFactory.class.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            return false;
        }

        if (canInitialize) {
            try {
                clazz.getConstructor().newInstance();
            } catch (Throwable t) {
                LOG.info("Unable to instantiate class {}", className, t);
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private static Constructor<LongAdder> getJava8LongAdderConstructor() {
        try {
            Class<?> clazz = LongAdderFactory.class.getClassLoader().loadClass(JAVA8_LONG_ADDER_CLASS_NAME);
            return (Constructor<LongAdder>) clazz.getConstructor();
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            LOG.info("Unable to locate nullary constructor for {}", JAVA8_LONG_ADDER_CLASS_NAME, e);
            return null;
        }
    }

    private LongAdderFactory() {
    }

}
