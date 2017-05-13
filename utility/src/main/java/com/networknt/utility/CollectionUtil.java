package com.networknt.utility;

import java.util.Collection;

/**
 * Utility to deal with collection
 *
 * @author Steve Hu
 */
public class CollectionUtil {
    @SuppressWarnings("rawtypes")
    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.size() == 0;
    }
}
