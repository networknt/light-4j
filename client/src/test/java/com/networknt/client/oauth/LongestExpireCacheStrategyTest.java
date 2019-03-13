package com.networknt.client.oauth;

import com.networknt.client.oauth.cache.LongestExpireCacheStrategy;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

public class LongestExpireCacheStrategyTest {
    private static LongestExpireCacheStrategy cacheStrategy =  new LongestExpireCacheStrategy(4);
    private static long initExpiryTime = System.currentTimeMillis();
    private static ArrayList<Jwt> initJwts = createJwts(4, initExpiryTime);
    @BeforeClass
    public static void setup() {
        for(Jwt jwt : initJwts) {
            cacheStrategy.cacheJwt(new Jwt.Key(jwt.getScopes()), jwt);
        }
    }

    @Test
    public void testCacheJwt() throws NoSuchFieldException, IllegalAccessException {
        Assert.assertNotNull(cacheStrategy.getCachedJwt(new Jwt.Key(initJwts.get(0).getScopes())));
        Field field = LongestExpireCacheStrategy.class.getDeclaredField("expiryQueue");
        field.setAccessible(true);
        PriorityBlockingQueue cachedQueue = (PriorityBlockingQueue) field.get(cacheStrategy);
        Field field1 = LongestExpireCacheStrategy.class.getDeclaredField("cachedJwts");
        field1.setAccessible(true);
        ConcurrentHashMap<Jwt.Key, Jwt> cachedJwts = (ConcurrentHashMap) field1.get(cacheStrategy);
        Assert.assertEquals(cachedJwts.size(), 4);
        Assert.assertEquals(cachedQueue.size(), 4);
        ArrayList<Jwt> jwts = createJwts(2, initExpiryTime + 10);
        Jwt jwt5 = jwts.get(0);
        Jwt jwt1 = cachedJwts.get(cachedQueue.peek());
        long originalExpiry = jwt1.getExpire();
        Assert.assertEquals(cachedJwts.get(new Jwt.Key(jwt1.getScopes())), jwt1);
        cacheStrategy.cacheJwt(new Jwt.Key(jwt5.getScopes()), jwt5);
        Assert.assertEquals(cachedJwts.get(new Jwt.Key(jwt5.getScopes())), jwt5);
        Assert.assertNotEquals(cachedJwts.get(new Jwt.Key(jwt5.getScopes())).getExpire(), originalExpiry);
    }

    //create an array of Jwts of number: num
    private static ArrayList<Jwt> createJwts(int num, long expiryTime) {

        ArrayList<Jwt> jwts = new ArrayList<>();
        for(int i = 0; i < num; i++) {
            Jwt jwt = new Jwt();
            jwt.setScopes(new HashSet<>(Arrays.asList(getScopes(i+1))));
            jwt.setExpire(expiryTime + i);
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
