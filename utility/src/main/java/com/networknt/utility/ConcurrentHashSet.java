/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.networknt.utility;

import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ConcurrentHashSet implementation
 *
 * @param <E> element
 */
public class ConcurrentHashSet<E> extends AbstractSet<E> implements Set<E>, java.io.Serializable {

    private static final long serialVersionUID = -8672117787651310382L;

    private static final Object PRESENT = new Object();

    private final ConcurrentMap<E, Object> map;

    public ConcurrentHashSet() {
        map = new ConcurrentHashMap<E, Object>();
    }

    public ConcurrentHashSet(int initialCapacity) {
        map = new ConcurrentHashMap<E, Object>(initialCapacity);
    }

    /**
     * Returns an iterator over the elements in this set. The elements are returned in no particular
     * order.
     *
     * @return an Iterator over the elements in this set
     * @see ConcurrentModificationException
     */
    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    /**
     * Returns the number of elements in this set (its cardinality).
     *
     * @return the number of elements in this set (its cardinality)
     */
    @Override
    public int size() {
        return map.size();
    }

    /**
     * Returns <i>true</i> if this set contains no elements.
     *
     * @return <i>true</i> if this set contains no elements
     */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Returns <i>true</i> if this set contains the specified element. More formally, returns
     * <i>true</i> if and only if this set contains an element <i>e</i> such that
     * <i>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</i>.
     *
     * @param o element whose presence in this set is to be tested
     * @return <i>true</i> if this set contains the specified element
     */
    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    /**
     * Adds the specified element to this set if it is not already present. More formally, adds the
     * specified element <i>e</i> to this set if this set contains no element <i>e2</i> such
     * that <i>(e==null&nbsp;?&nbsp;e2==null&nbsp;:&nbsp;e.equals(e2))</i>. If this set already
     * contains the element, the call leaves the set unchanged and returns <i>false</i>.
     *
     * @param e element to be added to this set
     * @return <i>true</i> if this set did not already contain the specified element
     */
    @Override
    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }

    /**
     * Removes the specified element from this set if it is present. More formally, removes an
     * element <i>e</i> such that <i>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</i>,
     * if this set contains such an element. Returns <i>true</i> if this set contained the element
     * (or equivalently, if this set changed as a result of the call). (This set will not contain
     * the element once the call returns.)
     *
     * @param o object to be removed from this set, if present
     * @return <i>true</i> if the set contained the specified element
     */
    @Override
    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }

    /**
     * Removes all of the elements from this set. The set will be empty after this call returns.
     */
    @Override
    public void clear() {
        map.clear();
    }

}
