package com.networknt.client.oauth;

import com.networknt.client.oauth.cache.LongestExpireCacheStrategy;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class LongestExpireCacheStrategyTest {
    private static LongestExpireCacheStrategy cacheStrategy =  new LongestExpireCacheStrategy(4);
    private static long initExpiryTime = System.currentTimeMillis();
    private static ArrayList<Jwt> initJwts = createJwts(4);;
    @BeforeClass
    public static void setup() {
        for(Jwt jwt : initJwts) {
            cacheStrategy.cacheJwt(new Jwt.Key(jwt.getScopes()), jwt);
        }
    }

    @Test
    public void testCacheJwt() {
        Assert.assertNotNull(cacheStrategy.getCachedJwt(new Jwt.Key(initJwts.get(0).getScopes())));
    }

    //create an array of Jwts of number: num
    private static ArrayList<Jwt> createJwts(int num) {

        ArrayList<Jwt> jwts = new ArrayList<>();
        for(int i = 0; i < num; i++) {
            Jwt jwt = new Jwt();
            jwt.setScopes(new HashSet<>(Arrays.asList(getScopes(i+1))));
            jwt.setExpire(initExpiryTime + i);
            jwts.add(jwt);
        }
        return jwts;
    }


    private static String[] getScopes(int numsOfScopes){
        String[] scopes = {"eat", "drink", "sleep", "study"};
        int length = numsOfScopes > scopes.length ? numsOfScopes%scopes.length : numsOfScopes;
        String[] result = new String[length];
        for(int i = 1; i <= length; i++) {
            result[i - 1] = scopes[i - 1];
        }
        return result;
    }
}
