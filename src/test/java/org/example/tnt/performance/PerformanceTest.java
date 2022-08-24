package org.example.tnt.performance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.tnt.classes.AggregationResponse;
import org.example.tnt.services.TntService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.BindException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.example.tnt.classes.TntExecutor.PARAMS_SEPARATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class PerformanceTest {

    private static final List<String> DEFAULT_SHIPMENTS = Arrays.asList("ship", "vessel");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TntService tntService;

    private AtomicInteger pricingRequestCounter;
    private AtomicInteger shipmentsRequestCounter;
    private AtomicInteger trackRequestCounter;


    @Before
    public void beforeTest() {
        log.info("Prepare the service");

        pricingRequestCounter = new AtomicInteger(0);
        shipmentsRequestCounter = new AtomicInteger(0);
        trackRequestCounter = new AtomicInteger(0);

        tntService.getPricingExecutor().setOnRemoteRequest(params -> {
            pricingRequestCounter.incrementAndGet();

            assertNotNull(params);

            String[] arr = params.split(PARAMS_SEPARATOR);

            Map<String, Double> res = new HashMap<>();
            for (String param : arr) {
                assertNotNull(param);
                res.put(param, 42.0);
            }

            return res;
        });

        tntService.getShipmentsExecutor().setOnRemoteRequest(params -> {
            shipmentsRequestCounter.incrementAndGet();

            assertNotNull(params);

            String[] arr = params.split(PARAMS_SEPARATOR);

            Map<String, List<String>> res = new HashMap<>();
            for (String param : arr) {
                assertNotNull(param);
                res.put(param, DEFAULT_SHIPMENTS);
            }

            return res;
        });


        tntService.getTrackExecutor().setOnRemoteRequest(params -> {
            trackRequestCounter.incrementAndGet();

            assertNotNull(params);

            String[] arr = params.split(PARAMS_SEPARATOR);

            Map<String, String> res = new HashMap<>();
            for (String param : arr) {
                assertNotNull(param);
                res.put(param, "track");
            }

            return res;
        });
    }


    @Test
    public void loadTest() throws InterruptedException {
        final int START_USERS = 500;
        final int DELTA_USERS = 500;
        final int MAX_USERS = 60000; // to be closer to max thread count

        int parallelRequestsCount = START_USERS;

        for (; parallelRequestsCount <= MAX_USERS; parallelRequestsCount = parallelRequestsCount + DELTA_USERS) {
            log.info("----------------------------------");
            log.info("|   Test for {} parallel users    |", parallelRequestsCount);
            log.info("----------------------------------");

            // waiting for a while to let OS will close sockets from previous step
            Thread.sleep(4000);

            PerformanceTestResult result = runLoad(parallelRequestsCount);
            assertNotNull(result);
            assertTrue(result.requests > 0);
            assertEquals(parallelRequestsCount, result.requests);
            assertTrue(result.pricingRequests > 0);
            assertTrue(result.shipmentsRequests > 0);
            assertTrue(result.trackRequests > 0);

            // calculate percentile (90%)
            int failPercentile = result.fail * 100 / result.requests;
            int timeoutPercentile = result.timeout * 100 / result.requests;

            int pricingPercentile = result.pricingRequests * 100 / result.requests;
            int shipmentsPercentile = result.shipmentsRequests * 100 / result.requests;
            int trackPercentile = result.trackRequests * 100 / result.requests;

            log.info("----------------------------------");
            log.info("  {} parallel users results:", parallelRequestsCount);
            log.info("  FAIL percentile:      {}", failPercentile);
            log.info("  TIMEOUT percentile:   {}", timeoutPercentile);
            log.info("  pricing percentile:   {}", pricingPercentile);
            log.info("  shipments percentile: {}", shipmentsPercentile);
            log.info("  track percentile:     {}", trackPercentile);
            log.info("----------------------------------");

            assertTrue(pricingPercentile <= 50);
            assertTrue(shipmentsPercentile <= 50);
            assertTrue(trackPercentile <= 50);

            boolean tooMuchFailedRequests = failPercentile >= 10;
            boolean tooMuchTimeoutRequests = timeoutPercentile >= 10;

            if (tooMuchFailedRequests || tooMuchTimeoutRequests) {
                log.info(">>> Your computer can run less than {} users in parallel", parallelRequestsCount);
                break;
            }
        }

        assertTrue(parallelRequestsCount >= 4500);
    }


    @Test
    public void stressTest() throws ExecutionException, InterruptedException {
        final int TIMEOUT_MS = 5000;
        final int MAX_REQUESTS = 10000;
        final int PARALLEL_REQUESTS = 500;

        AtomicInteger requestCounter = new AtomicInteger(0);
        AtomicInteger failCounter = new AtomicInteger(0);
        AtomicInteger timeoutCounter = new AtomicInteger(0);

        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(PARALLEL_REQUESTS);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 1; i <= MAX_REQUESTS; i++) {
            Future<?> future = executorService.submit(() -> {
                try {
                    requestCounter.incrementAndGet();

                    String url = "http://localhost:" + port + "/aggregation?pricing=NL,CN&track=109347263,123456891&shipments=109347263,123456891";

                    ResponseEntity<AggregationResponse> response;
                    long ts = System.currentTimeMillis();
                    try {
                        response = rest.exchange(url, HttpMethod.GET, null, AggregationResponse.class);
                    } finally {
                        ts = System.currentTimeMillis() - ts;
                        if (ts > TIMEOUT_MS) {
                            timeoutCounter.incrementAndGet();
                            log.warn("TIMEOUT");
                        }
                    }

                    assertNotNull(response);
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                } catch (Throwable t) {
                    failCounter.incrementAndGet();
                    throw t;
                }
            });
            futures.add(future);
        }

        for (Future<?> future : futures) {
            future.get();
        }

        executorService.shutdown();

        log.info("Requests (total):   {}", requestCounter.get());
        log.info("Requests (failed):  {}", failCounter.get());
        log.info("Requests (timeout): {}", timeoutCounter.get());
        log.info("Pricing requests:   {}", pricingRequestCounter.get());
        log.info("Shipments requests: {}", shipmentsRequestCounter.get());
        log.info("Track requests:     {}", trackRequestCounter.get());

        assertTrue(pricingRequestCounter.get() > 0);
        assertTrue(shipmentsRequestCounter.get() > 0);
        assertTrue(trackRequestCounter.get() > 0);

        assertEquals(MAX_REQUESTS, requestCounter.get());
        assertTrue(failCounter.get() < (MAX_REQUESTS * 0.1));
        assertTrue(timeoutCounter.get() < MAX_REQUESTS * 0.1);
    }


    private PerformanceTestResult runLoad(int requestMaxCount) throws InterruptedException {
        assertTrue(requestMaxCount > 0);

        log.info("Service port = {}", port);
        assertTrue(port > 0);

        final int TIMEOUT_MS = 5000;

        beforeTest();

        // prepare counters
        AtomicInteger requestCounter = new AtomicInteger(0);
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger failCounter = new AtomicInteger(0);
        AtomicInteger timeoutCounter = new AtomicInteger(0);


        // prepare threads
        //NOTE: real load-test should be initiated from another machines with emulation of different countries, networks, etc
        // also internal/external metrics should be collected
        List<Thread> threads = new ArrayList<>();
        for (int i = 1; i <= requestMaxCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    requestCounter.incrementAndGet();

                    String url = "http://localhost:" + port + "/aggregation?pricing=NL,CN&track=109347263,123456891&shipments=109347263,123456891";

                    ResponseEntity<AggregationResponse> response;
                    long ts = System.currentTimeMillis();
                    try {
                        response = rest.exchange(url, HttpMethod.GET, null, AggregationResponse.class);
                    } finally {
                        ts = System.currentTimeMillis() - ts;
                        if (ts > TIMEOUT_MS) {
                            timeoutCounter.incrementAndGet();
                            log.warn("TIMEOUT");
                        }
                    }

                    assertNotNull(response);
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    successCounter.incrementAndGet();
                } catch (Throwable t) {
                    failCounter.incrementAndGet();

                    if (t instanceof BindException) {
                        log.warn("OS cannot provide new socket");
                    } else {
                        throw t;
                    }
                }
            });
            thread.setName("load-test-" + i);
            threads.add(thread);
        }

        // warming up
        String url = "http://localhost:" + port + "/aggregation?pricing=NL,CN&track=109347263,123456891&shipments=109347263,123456891";
        ResponseEntity<AggregationResponse> response = rest.exchange(url, HttpMethod.GET, null, AggregationResponse.class);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());


        // pseudo-load-test
        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // analysing results
        log.info("Pricing requests:            {}", pricingRequestCounter.get());
        log.info("Shipments requests:          {}", shipmentsRequestCounter.get());
        log.info("Track requests:              {}", trackRequestCounter.get());
        log.info("External requests (total):   {}", requestCounter.get());
        log.info("External requests (success): {}", successCounter.get());
        log.info("External requests (fail):    {}", failCounter.get());
        log.info("External requests (timeout): {}", timeoutCounter.get());

        return new PerformanceTestResult(pricingRequestCounter.get(), shipmentsRequestCounter.get(), trackRequestCounter.get(),
                requestCounter.get(), successCounter.get(), failCounter.get(), timeoutCounter.get());
    }


    @Data
    @AllArgsConstructor
    private static class PerformanceTestResult {

        private int pricingRequests;
        private int shipmentsRequests;
        private int trackRequests;
        private int requests;
        private int success;
        private int fail;
        private int timeout;

    }

}
