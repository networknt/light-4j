package com.networknt.common;

/**
 * @param <T> The type of the first element
 * @param <T2> The type of the second element.
 *
 * @author Nicholas Azar
 */
public class Tuple<T, T2> {
    public final T first;
    public final T2 second;

    public Tuple(T first, T2 second) {
        this.first = first;
        this.second = second;
    }
}
