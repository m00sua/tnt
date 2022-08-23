package org.example.tnt.classes;

import org.example.tnt.TestUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.example.tnt.TestUtils.expectIllegalArgumentException;
import static org.example.tnt.classes.TntExecutor.PARAMS_SEPARATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class TntExecutorTest {

    @Test
    public void TntExecutorConstructorNegativeTest() {
        expectIllegalArgumentException(() -> new TntExecutor<>(null, -1, -1, null, null), "Name cannot be blank");
        expectIllegalArgumentException(() -> new TntExecutor<>("someName", -1, -1, null, null), "Max Size must be positive");
        expectIllegalArgumentException(() -> new TntExecutor<>("someName", 1, 0, null, null), "Timeout must be positive");
        expectIllegalArgumentException(() -> new TntExecutor<>("someName", 1, 1, null, null), "Result Monitor cannot be null");
        expectIllegalArgumentException(() -> new TntExecutor<>("someName", 1, 1, new Object(), null), "onRemoteRequest cannot be null");
    }


    @Test
    public void TntExecutorCatchErrorTest() {
        TntExecutor<String> executor = new TntExecutor<>("test", 3, 10, new Object(),
                params -> {
                    throw new IllegalStateException("Should be handled");
                });

        executor.addItems("1");
        executor.addItems("2");
        executor.addItems("3");
    }



    @Test
    public void TntExecutorSizeReachedTest() {
        Object monitor = new Object();

        AtomicInteger callCounter = new AtomicInteger(0);

        TntExecutor<String> executor = new TntExecutor<>("test", 3, 10, monitor,
                params -> {
                    callCounter.incrementAndGet();

                    assertNotNull(params);

                    String[] arr = params.split(PARAMS_SEPARATOR);

                    Map<String, String> res = new HashMap<>();
                    for (String param : arr) {
                        assertNotNull(param);
                        res.put(param, param + "_1");
                    }

                    return res;
                });

        Map<String, String> resultMap = executor.createResultMap(new String[]{"0", "1"});
        assertNotNull(resultMap);
        assertEquals(2, resultMap.size());
        assertTrue(resultMap.containsKey("0"));
        assertTrue(resultMap.containsKey("1"));
        assertNull(resultMap.get("0"));
        assertNull(resultMap.get("1"));

        // check for correct params
        executor.addItems("0");
        assertEquals(0, callCounter.get());

        executor.addItems("1");
        assertEquals(0, callCounter.get());

        executor.addItems("2");
        assertEquals(1, callCounter.get());

        executor.grabResults(resultMap);
        assertEquals(2, resultMap.size());
        assertTrue(resultMap.containsKey("0"));
        assertTrue(resultMap.containsKey("1"));
        assertEquals("0_1", resultMap.get("0"));
        assertEquals("1_1", resultMap.get("1"));
    }


    @Test
    public void TntExecutorWrongParamsTest() {
        Object monitor = new Object();

        AtomicInteger callCounter = new AtomicInteger(0);

        TntExecutor<String> executor = new TntExecutor<>("test", 3, 10, monitor,
                params -> {
                    callCounter.incrementAndGet();

                    assertNotNull(params);

                    String[] arr = params.split(PARAMS_SEPARATOR);

                    Map<String, String> res = new HashMap<>();
                    for (String param : arr) {
                        assertNotNull(param);
                        res.put(param, param + "_1");
                    }

                    return res;
                });

        Map<String, String> resultMap = executor.createResultMap(new String[]{"0", "1"});
        assertNotNull(resultMap);
        assertEquals(2, resultMap.size());
        assertTrue(resultMap.containsKey("0"));
        assertTrue(resultMap.containsKey("1"));
        assertNull(resultMap.get("0"));
        assertNull(resultMap.get("1"));

        // check that no data yet
        executor.grabResults(resultMap);
        assertEquals(2, resultMap.size());
        assertTrue(resultMap.containsKey("0"));
        assertTrue(resultMap.containsKey("1"));
        assertNull(resultMap.get("0"));
        assertNull(resultMap.get("1"));

        // check for wrong params
        executor.addItems("aaa");
        assertEquals(0, callCounter.get());

        executor.addItems("bbb");
        assertEquals(0, callCounter.get());

        executor.addItems("ccc");
        assertEquals(1, callCounter.get());

        executor.grabResults(resultMap);
        assertEquals(2, resultMap.size());
        assertTrue(resultMap.containsKey("0"));
        assertTrue(resultMap.containsKey("1"));
        assertNull(resultMap.get("0"));
        assertNull(resultMap.get("1"));
    }

}
