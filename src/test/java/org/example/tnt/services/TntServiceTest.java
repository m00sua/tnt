package org.example.tnt.services;

import org.example.tnt.classes.AggregationResponse;
import org.example.tnt.clients.TntClients;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;


@SpringBootTest
@RunWith(SpringRunner.class)
public class TntServiceTest {

    @Test
    public void negativeTest() {
        try {
            new TntService(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("TNT Client cannot be null", e.getMessage());
        }
    }


    @Test
    public void aggregateTest() {
        TntService tntService = new TntService(createMock());
        AggregationResponse res = tntService.aggregate("AA", "BB", "any");
        assertNotNull(res);

        Map<String, Double> pricing = res.getPricing();
        assertNotNull(pricing);
        assertEquals(2, pricing.size());
        assertEquals(Double.valueOf(85.12639391383956), pricing.get("CN"));
        assertEquals(Double.valueOf(52.17154406547613), pricing.get("NL"));

        Map<String, List<String>> shipments = res.getShipments();
        assertNull(shipments);

        Map<String, String> tracking = res.getTracking();
        assertNull(tracking);
    }


    private TntClients.TntClient createMock() {

        return new TntClients.TntClient() {
            @Override
            public Map<String, List<String>> shipments(@Valid String params) {
                if ("BB".equals(params)) {
                    return null;
                }
                throw new IllegalArgumentException();
            }

            @Override
            public Map<String, String> track(@Valid String params) {
                throw new IllegalStateException();
            }

            @Override
            public Map<String, Double> pricing(@Valid String params) {
                if ("AA".equals(params)) {
                    return new HashMap<String, Double>() {{
                        put("CN", 85.12639391383956);
                        put("NL", 52.17154406547613);
                    }};
                }
                throw new IllegalArgumentException();
            }
        };
    }

}
