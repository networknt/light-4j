package com.networknt.utility;

import java.util.Collection;

/**
 * Created by stevehu on 2017-01-09.
 */
public class CollectionUtil {
    @SuppressWarnings("rawtypes")
    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.size() == 0;
    }
}
