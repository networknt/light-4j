package com.networknt.balance;

import com.networknt.registry.URL;

import java.util.List;

public interface LoadBalance {
    // select one from a list of URLs

    /**
     * Select one url from a list of url with requestKey as optional.
     *
     * @param urls List
     * @param requestKey String
     * @return URL
     */
    URL select(List<URL> urls, String requestKey);

    /**
     * return positive int value of originValue
     * @param originValue original value
     * @return positive int
     */
    default int getPositive(int originValue){
        return 0x7fffffff & originValue;
    }

}
