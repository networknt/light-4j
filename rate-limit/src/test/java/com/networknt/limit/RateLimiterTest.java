package com.networknt.limit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        limitConfig = LimitConfig.load();
        rateLimiter = new RateLimiter(limitConfig);
        limitConfig.setKey(LimitKey.ADDRESS);
        rateLimiterAddress = new RateLimiter(limitConfig);
        limitConfig.setKey(LimitKey.CLIENT);
        rateLimiterClient = new RateLimiter(limitConfig);
    }

    @Test
    public void testByServer() throws Exception{
        List<RateLimitResponse> responseList = new ArrayList<>();
        Callable<RateLimitResponse> task =this::callByServerAsync;
        List<Callable<RateLimitResponse>> tasks = Collections.nCopies(12, task);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
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
        return rateLimiter.isAllowByServer(limitQuota, "/v1/address");
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
        return rateLimiterClient.isAllowDirect(clientId, rateLimit, RateLimiter.CLIENT_TYPE);
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
        return rateLimiterAddress.isAllowByPath(address, "/v1/address", RateLimiter.ADDRESS_TYPE);
    }

}
