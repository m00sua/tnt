package org.example.tnt.services;

import org.example.tnt.classes.AggregationResponse;
import org.example.tnt.clients.TntClients;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.example.tnt.TestUtils.expectIllegalArgumentException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@SpringBootTest
@RunWith(SpringRunner.class)
public class TntServiceTest {

    @Test
    public void negativeTest() {
        expectIllegalArgumentException(() -> new TntService(null), "TNT Client cannot be null");
    }


    @Test
    public void aggregateTest() {
        TntService tntService = new TntService(createMock());
        AggregationResponse res = tntService.aggregate("AA,BB,CC,DD,EE", "AA,BB,CC,DD,EE", "AA,BB,CC,DD,EE");
        assertNotNull(res);

        Map<String, Double> pricing = res.getPricing();
        assertNotNull(pricing);
        assertEquals(5, pricing.size());
        assertEquals(Double.valueOf(85.12639391383956), pricing.get("AA"));
        assertEquals(Double.valueOf(52.17154406547613), pricing.get("BB"));
        assertEquals(Double.valueOf(42.0), pricing.get("CC"));
        assertEquals(Double.valueOf(43.0), pricing.get("DD"));
        assertEquals(Double.valueOf(44.1), pricing.get("EE"));

        Map<String, List<String>> shipments = res.getShipments();
        assertNotNull(shipments);
        assertEquals(5, shipments.size());

        Map<String, String> tracking = res.getTracking();
        assertNotNull(tracking);
        assertEquals(5, tracking.size());

    }


    private TntClients.TntClient createMock() {

        return new TntClients.TntClient() {
            @Override
            public Map<String, List<String>> shipments(@Valid String params) {
                if ("AA,BB,CC,DD,EE".equals(params)) {
                    return new HashMap<String, List<String>>(){{
                        put("AA", Arrays.asList("14", "15"));
                        put("BB", Arrays.asList("24", "25"));
                        put("CC", Arrays.asList("34", "35"));
                        put("DD", Arrays.asList("44", "45"));
                        put("EE", Arrays.asList("54", "55"));

                    }};
                }
                throw new IllegalArgumentException();
            }

            @Override
            public Map<String, String> track(@Valid String params) {
                if ("AA,BB,CC,DD,EE".equals(params)) {
                    return new HashMap<String, String>() {{
                        put("AA", "12639391383956");
                        put("BB", "17154406547613");
                        put("CC", "420");
                        put("DD", "430");
                        put("EE", "441");
                    }};
                }
                throw new IllegalArgumentException();
            }

            @Override
            public Map<String, Double> pricing(@Valid String params) {
                if ("AA,BB,CC,DD,EE".equals(params)) {
                    return new HashMap<String, Double>() {{
                        put("AA", 85.12639391383956);
                        put("BB", 52.17154406547613);
                        put("CC", 42.0);
                        put("DD", 43.0);
                        put("EE", 44.1);
                    }};
                }
                throw new IllegalArgumentException();
            }
        };
    }

}
