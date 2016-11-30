package com.networknt.service;

import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.Introspector;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.Provider;
import java.util.*;

/**
 * Created by stevehu on 2016-11-26.
 */
public class SingletonServiceFactory {
    static String CONFIG_NAME = "service";
    static Logger logger = LoggerFactory.getLogger(SingletonServiceFactory.class);

    private static Map<Class, Object> serviceMap = new HashMap<>();

    static {
        ServiceConfig serviceConfig =
                (ServiceConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ServiceConfig.class);
        List<Map<String, List<Object>>> singletons = serviceConfig.getSingletons();
        try {
            if(singletons != null && singletons.size() > 0) {
                for(Map<String, List<Object>> singleton: singletons) {
                    Iterator it = singleton.entrySet().iterator();
                    if (it.hasNext()) {
                        Map.Entry<String, List<Object>> pair = (Map.Entry)it.next();
                        String key = pair.getKey();
                        List<Object> value = pair.getValue();
                        handleSingleton(key, value);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public static void handleSingleImpl(List<Class> interfaceClasses, List<Object> value) throws ClassNotFoundException, Exception {
        Object object = value.get(0);
        if(object instanceof String) {
            Class implClass = Class.forName((String)object);
            Object obj = construct(implClass);
            for(Class c: interfaceClasses) {
                serviceMap.put(c, obj);  // all interfaces share the same impl
            }
        } else {
            // map of impl class and properties.
            Map<String, Map<String, Object>> map = (Map<String, Map<String, Object>>)object;
            // construct it using default construct and call all set methods with values defined in the properties
            Iterator it = map.entrySet().iterator();
            if (it.hasNext()) {
                Map.Entry<String, Map<String, Object>> pair = (Map.Entry) it.next();
                String key = pair.getKey();
                Map<String, Object> properties = pair.getValue();
                Class implClass = Class.forName(key);
                Object obj = construct(implClass);

                Method[] allMethods = implClass.getMethods();
                for(Method method : allMethods) {

                    if(method.getName().startsWith("set")) {
                        Object [] o = new Object [1];
                        String propertyName = Introspector.decapitalize(method.getName().substring(3));
                        Object v = properties.get(propertyName);
                        if(v == null) {
                            // it is not primitive type, so find the object in service map.
                            Class<?>[] pType  = method.getParameterTypes();
                            v = serviceMap.get(pType[0]);
                        }
                        o[0] = v;
                        method.invoke(obj, o);
                    }
                }
                for(Class c: interfaceClasses) {
                    serviceMap.put(c, obj);  // all interfaces share the same impl
                }
            }
        }
    }

    public static void handleMultipleImpl(List<Class> interfaceClasses, List<Object> value) throws ClassNotFoundException, Exception {

        List<Object> arrays = new ArrayList();
        for(Class c: interfaceClasses) {
            arrays.add(Array.newInstance(c, value.size()));
        }
        for(int i = 0; i < value.size(); i++) {
            Object object = value.get(i);
            if(object instanceof String) {
                Class implClass = Class.forName((String)value.get(i));
                for(int j = 0; j < arrays.size(); j++) {
                    Array.set(arrays.get(j), i, construct(implClass));
                }
            } else {
                // map of impl class and properties

            }
        }
        for(int i = 0; i < interfaceClasses.size(); i++) {
            serviceMap.put(interfaceClasses.get(i), arrays.get(i));
        }

    }
    public static void handleSingleton(String key, List<Object> value) throws Exception {

        List<Class> interfaceClasses = new ArrayList();
        if(key.contains(",")) {
            String[] interfaces = key.split(",");
            for(int i = 0; i < interfaces.length; i++) {
                interfaceClasses.add(Class.forName(interfaces[i]));
            }
        } else {
            interfaceClasses.add(Class.forName(key));
        }
        // the value can be a list of implementation class names or a map.
        if(value != null && value.size() == 1) {
            handleSingleImpl(interfaceClasses, value);
        } else {
            handleMultipleImpl(interfaceClasses, value);
        }
    }

    public static Object construct(Class clazz) throws Exception {
        // find out how many constructors this class has.
        Object instance  = null;
        Constructor[] allConstructors = clazz.getDeclaredConstructors();
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
                if(!beanFound) {
                    // could not find all parameters, try another constructor.
                    break;
                } else {
                    // this constructor parameters are found.
                    instance = ctor.newInstance(params);
                }
            } else {
                instance = clazz.newInstance();
            }
        }
        return instance;

    }

    public static Object getBean(Class interfaceClass) {
       return serviceMap.get(interfaceClass);
    }
}
