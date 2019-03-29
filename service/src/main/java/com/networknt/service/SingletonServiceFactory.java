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

package com.networknt.service;

import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.Introspector;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by steve on 2016-11-26.
 */
@SuppressWarnings("unchecked")
public class SingletonServiceFactory {
    private static String CONFIG_NAME = "service";
    private static Logger logger = LoggerFactory.getLogger(SingletonServiceFactory.class);

    private static Map<String, Object> serviceMap = new HashMap<>();
    // map is statically loaded to make sure there is only one instance per jvm
    static {
        ServiceConfig serviceConfig =
                (ServiceConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ServiceConfig.class);
        List<Map<String, Object>> singletons = serviceConfig.getSingletons();
        //logger.debug("singletons " + singletons);
        try {
            if(singletons != null && singletons.size() > 0) {
                // for each interface or class, there might be a list of implementations or one init class::method
                for(Map<String, Object> singleton: singletons) {
                    Iterator it = singleton.entrySet().iterator();
                    if (it.hasNext()) {
                        Map.Entry<String, Object> pair = (Map.Entry)it.next();
                        String key = pair.getKey();
                        key = key.replaceAll("\\s+","");
                        Object value = pair.getValue();
                        if(value instanceof List) {
                            handleSingletonList(key, (List)value);
                        } else if(value instanceof String) {
                            handleSingletonClass(key, (String)value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    private static Object handleSingleImpl(List<String> interfaceClasses, List<Object> value) throws Exception {
        // only one object should be defined in value. TODO throws exception if number of object is not correct.
        Object object = value.get(0);
        if(object instanceof String) {
            Class implClass = Class.forName((String)object);
            Object obj = construct(implClass);
            for(String c: interfaceClasses) {
                serviceMap.put(c, obj);  // all interfaces share the same impl
            }
            return obj;
        }
        // map of impl class and properties.
        Map<String, Map<String, Object>> map = (Map<String, Map<String, Object>>)object;
        //logger.debug("map = " + map);
        // construct it using default construct and call all set methods with values defined in the properties
        return constructAndAddToServiceMap(interfaceClasses, map);
    }

    /**
     * @param interfaceClasses A list of the interfaces implemented by the map. (usually just one though)
     * @param map Mapping of name of concrete class to its fields (could be field name : value, or list of types to value).
     * @throws Exception
     */
    private static List<Object> constructAndAddToServiceMap(List<String> interfaceClasses, Map map) throws Exception {
        Iterator it = map.entrySet().iterator();
        List<Object> items = new ArrayList<>();
        if (it.hasNext()) {
            Map.Entry<String, Map<String, Object>> pair = (Map.Entry) it.next();
            String key = pair.getKey();
            Class implClass = Class.forName(key);
            Object mapOrList = pair.getValue();
            // at this moment, pair.getValue() has two scenarios,
            // 1. map that can be used to set properties after construct the object with reflection.
            // 2. list that can be used by matched constructor to create the instance.
            Object obj;
            if(mapOrList instanceof Map) {
                obj = construct(implClass);

                Method[] allMethods = implClass.getMethods();
                for(Method method : allMethods) {

                    if(method.getName().startsWith("set")) {
                        //logger.debug("method name " + method.getName());
                        Object [] o = new Object [1];
                        String propertyName = Introspector.decapitalize(method.getName().substring(3));
                        Object v = ((Map)mapOrList).get(propertyName);
                        if(v == null) {
                            // it is not primitive type, so find the object in service map.
                            Class<?>[] pType  = method.getParameterTypes();
                            v = serviceMap.get(pType[0].getName());
                        }
                        if(v != null) {

                            o[0] = v;
                            method.invoke(obj, o);
                        }
                    }
                }
            } else if(mapOrList instanceof List){
                obj = ServiceUtil.constructByParameterizedConstructor(implClass, (List)mapOrList);
            } else {
                throw new RuntimeException("Only Map or List is allowed for implementation parameters, null provided.");
            }
            items.add(obj);

            for(String c: interfaceClasses) {
                serviceMap.put(c, obj);  // all interfaces share the same impl
            }
        }
        return items;
    }

    private static void handleMultipleImpl(List<String> interfaceClasses, List<Object> value) throws Exception {

        List<Object> arrays = interfaceClasses.stream()
                .map(c -> { try {
                    return Array.newInstance(Class.forName(c), value.size());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("ClassNotFoundException for " + c, e);
                }
                })
                .collect(Collectors.toList());
        for(int i = 0; i < value.size(); i++) {
            Object object = value.get(i);
            if(object instanceof String) {
                Class implClass = Class.forName((String)value.get(i));
                for (Object array : arrays) {
                    Array.set(array, i, construct(implClass));
                }
            } else if (object instanceof Map) {
                Map<String, Map<String, Object>> map = (Map<String, Map<String, Object>>)object;
                List<Object> constructedClasses = constructAndAddToServiceMap(interfaceClasses, map);
                for (Object array : arrays) {
                    Array.set(array, i, constructedClasses.get(0));
                }
            }
        }
        for(int i = 0; i < interfaceClasses.size(); i++) {
            serviceMap.put(interfaceClasses.get(i), arrays.get(i));
        }
    }

    /**
     * For each singleton definition, create object with the initializer class and method,
     * and push it into the service map with the key of the class name.
     *
     * @param key String class name of the object that needs to be initialized
     * @param value String class name of initializer class and method separated by "::"
     * @throws Exception exception thrown from the object creation
     */
    private static void handleSingletonClass(String key, String value) throws Exception {
        Object object = handleValue(value);
        if(key.contains(",")) {
            String[] interfaces = key.split(",");
            for (String anInterface : interfaces) {
                serviceMap.put(anInterface, object);
            }
        } else {
            serviceMap.put(key, object);
        }
    }

    private static Object handleValue(String value) throws Exception {
        if(value.contains("::")) {
            String initClassName = value.substring(0, value.indexOf("::"));
            String initMethodName = value.substring(value.indexOf("::") + 2);
            Class initClass = Class.forName(initClassName);
            Object obj = construct(initClass);
            Method method = obj.getClass().getMethod(initMethodName);
            return method.invoke(obj);
        } else {
            throw new RuntimeException("No initializer method defined for " + value);
        }
    }

    /**
     * For each singleton definition, create object for the interface with the implementation class,
     * and push it into the service map with key and implemented object.
     * @param key String interface or multiple interface separated by ","
     * @param value List of implementations of interface(s) defined in the key
     * @throws Exception exception thrown from the object creation
     */
    private static void handleSingletonList(String key, List<Object> value) throws Exception {

        List<String> interfaceClasses = new ArrayList();
        if(key.contains(",")) {
            String[] interfaces = key.split(",");
            interfaceClasses.addAll(Arrays.asList(interfaces));
        } else {
            interfaceClasses.add(key);
        }
        // the value can be a list of implementation class names or a map.
        if(value != null && value.size() == 1) {
            handleSingleImpl(interfaceClasses, value);
        } else {
            handleMultipleImpl(interfaceClasses, value);
        }
    }

    private static Object construct(Class clazz) throws Exception {
        // find out how many constructors this class has.
        Object instance  = null;
        Constructor[] allConstructors = clazz.getDeclaredConstructors();
        // iterate all constructors of this class and try each non-default one with parameters from service map
        // also flag if there is default constructor without argument.
        boolean hasDefaultConstructor = false;
        for (Constructor ctor : allConstructors) {
            Class<?>[] pType  = ctor.getParameterTypes();
            if(pType.length > 0) {
                boolean beanFound = true;
                Object[] params = new Object[pType.length];
                for (int j = 0; j < pType.length; j++) {
                    //System.out.println("pType = " + pType[j]);
                    Object obj = getBean(pType[j]);
                    if(obj != null) {
                        params[j] = obj;
                    } else {
                        // cannot find parameter in singleton service map, skip this constructor.
                        beanFound = false;
                        break;
                    }
                }
                if(beanFound) {
                    // this constructor parameters are found.
                    instance = ctor.newInstance(params);
                    break;
                }
            } else {
                hasDefaultConstructor = true;
            }
        }
        if(instance != null) {
            return instance;
        } else {
            if(hasDefaultConstructor) {
                return clazz.getConstructor().newInstance();
            } else {
                // error that no instance can be created.
                throw new Exception("No instance can be created for class " + clazz);
            }
        }
    }


    /**
     * Get a cached singleton object from service map by interface class and generic type class.
     * The serviceMap is constructed from service.yml which defines interface and generic type
     * to implementation mapping.
     *
     * @param interfaceClass Interface class
     * @param <T> class type
     * @param typeClass Generic type class
     * @return The implementation object
     */
    public static <T> T getBean(Class<T> interfaceClass, Class typeClass) {
        Object object = serviceMap.get(interfaceClass.getName() + "<" + typeClass.getName() + ">");
        if(object == null) return null;
        if(object instanceof Object[]) {
            return (T)Array.get(object, 0);
        } else {
            return (T)object;
        }
    }

    /**
     * Get a cached singleton object from service map by interface class. The serviceMap
     * is constructed from service.yml which defines interface to implementation mapping.
     *
     * As in the service.yml one interface can have several implementations and that might be
     * the case for this method to get the first one only if there are multiple.
     *
     * @param interfaceClass Interface class
     * @param <T> class type
     * @return The implementation object
     */
    public static <T> T getBean(Class<T> interfaceClass) {
        Object object = serviceMap.get(interfaceClass.getName());
        if(object == null) return null;
        if(object instanceof Object[]) {
            return (T)Array.get(object, 0);
        } else {
            return (T)object;
        }
    }

    /**
     * Get a list of cached singleton objects from service map by interface class. If there
     * is only one object in the serviceMap, then construct the list with this only object.
     *
     * @param interfaceClass Interface class
     * @param <T> class type
     * @return The array of implementation objects
     */
    public static <T> T[] getBeans(Class<T> interfaceClass) {
        Object object = serviceMap.get(interfaceClass.getName());
        if(object == null) return null;
        if(object instanceof Object[]) {
            return (T[])object;
        } else {
            Object array = Array.newInstance(interfaceClass, 1);
            Array.set(array, 0, object);
            return (T[])array;
        }
    }

    /**
     * This is a testing API that you can manipulate serviceMap by inject an object
     * into it programmatically. It is not recommended to use it for the normal code
     * but just for test cases in order to simulate certain scenarios.
     *
     * @param className full classname with package
     * @param object The object that you want to binding to the class
     */
    public static void setBean(String className, Object object) {
        serviceMap.put(className, object);
    }
}
