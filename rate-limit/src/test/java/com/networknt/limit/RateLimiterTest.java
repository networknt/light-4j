package com.networknt.limit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class RateLimiterTest {

    private static RateLimiter rateLimiter;
    private static RateLimiter rateLimiterClient;
    private static RateLimiter rateLimiterAddress;

    private static LimitConfig limitConfig;

    @Before
    public void setUp() throws Exception{
        LimitConfig.load();
        limitConfig = LimitConfig.load();
        rateLimiter = new RateLimiter(limitConfig);
        limitConfig.setKey(LimitKey.ADDRESS);
        rateLimiterAddress = new RateLimiter(limitConfig);
        limitConfig.setKey(LimitKey.CLIENT);
        rateLimiterClient = new RateLimiter(limitConfig);
    }

    @org.junit.After
    public void tearDown() {
        com.networknt.config.Config.getInstance().clear();
    }

    @Test
    public void testByServer() throws Exception{
        List<RateLimitResponse> responseList = new ArrayList<>();
        Callable<RateLimitResponse> task =this::callByServerAsync;
        List<Callable<RateLimitResponse>> tasks = Collections.nCopies(12, task);

        //change the thread number here to test multi-threads
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        List<Future<RateLimitResponse>> futures = executorService.invokeAll(tasks);
        for (Future<RateLimitResponse> future : futures) {
            responseList.add(future.get());
        }
//        LimitQuota limitQuota = limitConfig.getServer().get("/v1/address");
//           for (int i=0; i<12; i++) {
//            responseList.add(rateLimiter.isAllowByServer(limitQuota, "/v1/address"));
//        }

       // Assert.assertEquals(responseList.size(), 12);
        List<RateLimitResponse> rejects = responseList.stream().filter(r->!r.isAllow()).collect(Collectors.toList());
        Assert.assertEquals(rejects.size(), 2);
        executorService.shutdown();
    }

    public RateLimitResponse callByServerAsync() throws Exception {
        LimitQuota limitQuota = limitConfig.getServer().get("/v1/address");
        return rateLimiter.isAllowByServer( "/v1/address", limitConfig);
    }

    /**
     * Test with a longer request path with the configuration /v1/address only as path prefix.
     * @throws Exception
     */
    @Test
    public void testByServerWithPathPrefix() throws Exception{
        List<RateLimitResponse> responseList = new ArrayList<>();
        Callable<RateLimitResponse> task =this::callByServerAsyncWithLongPath;
        List<Callable<RateLimitResponse>> tasks = Collections.nCopies(12, task);

        //change the thread number here to test multi-threads
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        List<Future<RateLimitResponse>> futures = executorService.invokeAll(tasks);
        for (Future<RateLimitResponse> future : futures) {
            responseList.add(future.get());
        }
//        LimitQuota limitQuota = limitConfig.getServer().get("/v1/address");
//           for (int i=0; i<12; i++) {
//            responseList.add(rateLimiter.isAllowByServer(limitQuota, "/v1/address"));
//        }

        // Assert.assertEquals(responseList.size(), 12);
        List<RateLimitResponse> rejects = responseList.stream().filter(r->!r.isAllow()).collect(Collectors.toList());
        Assert.assertEquals(rejects.size(), 2);
        executorService.shutdown();
    }

    public RateLimitResponse callByServerAsyncWithLongPath() throws Exception {
        LimitQuota limitQuota = limitConfig.getServer().get("/v1/address");
        return rateLimiter.isAllowByServer( "/v1/address/anything/else", limitConfig);
    }

    @Test
    public void testByClient() throws Exception{
        List<RateLimitResponse> responseList = new ArrayList<>();
        Callable<RateLimitResponse> task =this::callByClientAsync;
        List<Callable<RateLimitResponse>> tasks = Collections.nCopies(12, task);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        List<Future<RateLimitResponse>> futures = executorService.invokeAll(tasks);
        for (Future<RateLimitResponse> future : futures) {
            responseList.add(future.get());
        }
        List<RateLimitResponse> rejects = responseList.stream().filter(r->!r.isAllow()).collect(Collectors.toList());
        Assert.assertEquals(rejects.size(), 2);
        executorService.shutdown();

    }

    public RateLimitResponse callByClientAsync() throws Exception {
        String clientId = "f7d42348-c647-4efb-a52d-4c5787421e74";
        List<LimitQuota> rateLimit = limitConfig.getClient().directMaps.get(clientId);
        return rateLimiterClient.isAllowDirect(clientId, "/v1/petstore", RateLimiter.CLIENT_TYPE, limitConfig);
    }


    @Test
    public void testByAddress() throws Exception{
        List<RateLimitResponse> responseList = new ArrayList<>();
        Callable<RateLimitResponse> task =this::callByAddressAsync;
        List<Callable<RateLimitResponse>> tasks = Collections.nCopies(12, task);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        List<Future<RateLimitResponse>> futures = executorService.invokeAll(tasks);
        for (Future<RateLimitResponse> future : futures) {
            responseList.add(future.get());
        }
        List<RateLimitResponse> rejects = responseList.stream().filter(r->!r.isAllow()).collect(Collectors.toList());
        Assert.assertEquals(rejects.size(), 2);
        executorService.shutdown();

    }

    public RateLimitResponse callByAddressAsync() throws Exception {
        String address = "192.168.1.102";
        return rateLimiterAddress.isAllowDirect(address, "/v1/address", RateLimiter.ADDRESS_TYPE, limitConfig);
    }

    @Test
    public void testByServerMemoryLeak() throws Exception {
        List<RateLimitResponse> responseList = new ArrayList<>();
        Callable<RateLimitResponse> task =this::callByServerAsyncRandom;
        List<Callable<RateLimitResponse>> tasks = Collections.nCopies(12, task);

        //change the thread number here to test multi-threads
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        List<Future<RateLimitResponse>> futures = executorService.invokeAll(tasks);
        for (Future<RateLimitResponse> future : futures) {
            responseList.add(future.get());
        }

        // Assert.assertEquals(responseList.size(), 12);
        List<RateLimitResponse> rejects = responseList.stream().filter(r->!r.isAllow()).collect(Collectors.toList());
        Assert.assertEquals(rejects.size(), 2);
        executorService.shutdown();
    }

    // Method to generate a random string of a given length
    public static String generateRandomString(int length) {
        // Characters to choose from
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);

        // Loop to append random characters to the StringBuilder
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            sb.append(characters.charAt(randomIndex));
        }

        return sb.toString();
    }

    public RateLimitResponse callByServerAsyncRandom() throws Exception {
        LimitQuota limitQuota = limitConfig.getServer().get("/v1/" + generateRandomString(10));
        return rateLimiter.isAllowByServer( "/v1/" + generateRandomString(10), limitConfig);
    }

    @Test
    public void testByAddressMemoryLeak() throws Exception{
        List<RateLimitResponse> responseList = new ArrayList<>();
        Callable<RateLimitResponse> task =this::callByAddressAsyncRandom;
        List<Callable<RateLimitResponse>> tasks = Collections.nCopies(12, task);
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        List<Future<RateLimitResponse>> futures = executorService.invokeAll(tasks);
        for (Future<RateLimitResponse> future : futures) {
            responseList.add(future.get());
        }
        List<RateLimitResponse> rejects = responseList.stream().filter(r->!r.isAllow()).collect(Collectors.toList());
        Assert.assertEquals(rejects.size(), 2);
        executorService.shutdown();

    }

    public RateLimitResponse callByAddressAsyncRandom() throws Exception {
        String address = "192.168.1.102";
        return rateLimiterAddress.isAllowDirect(address, "/v1/" + generateRandomString(10), RateLimiter.ADDRESS_TYPE, limitConfig);
    }


    @Test
    public void testByClientRandom() throws Exception{
        List<RateLimitResponse> responseList = new ArrayList<>();
        Callable<RateLimitResponse> task =this::callByClientAsyncRandom;
        List<Callable<RateLimitResponse>> tasks = Collections.nCopies(12, task);
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        List<Future<RateLimitResponse>> futures = executorService.invokeAll(tasks);
        for (Future<RateLimitResponse> future : futures) {
            responseList.add(future.get());
        }
        List<RateLimitResponse> rejects = responseList.stream().filter(r->!r.isAllow()).collect(Collectors.toList());
        Assert.assertEquals(rejects.size(), 2);
        executorService.shutdown();

    }

    public RateLimitResponse callByClientAsyncRandom() throws Exception {
        String clientId = "f7d42348-c647-4efb-a52d-4c5787421e75";
        List<LimitQuota> rateLimit = limitConfig.getClient().directMaps.get(clientId);
        return rateLimiterClient.isAllowDirect(clientId, "/v1/" + generateRandomString(10), RateLimiter.CLIENT_TYPE, limitConfig);
    }

    @Test
    public void testByUserRandom() throws Exception{
        List<RateLimitResponse> responseList = new ArrayList<>();
        Callable<RateLimitResponse> task =this::callByUserAsyncRandom;
        List<Callable<RateLimitResponse>> tasks = Collections.nCopies(12, task);
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        List<Future<RateLimitResponse>> futures = executorService.invokeAll(tasks);
        for (Future<RateLimitResponse> future : futures) {
            responseList.add(future.get());
        }
        List<RateLimitResponse> rejects = responseList.stream().filter(r->!r.isAllow()).collect(Collectors.toList());
        Assert.assertEquals(rejects.size(), 2);
        executorService.shutdown();

    }

    public RateLimitResponse callByUserAsyncRandom() throws Exception {
        String userId = "albert@lightapi.net";
        List<LimitQuota> rateLimit = limitConfig.getUser().directMaps.get(userId);
        return rateLimiterClient.isAllowDirect(userId, "/v1/" + generateRandomString(10), RateLimiter.USER_TYPE, limitConfig);
    }

    @Test
    public void testRetryAfter() {

    }

}
