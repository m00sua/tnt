package org.example.tnt.clients;

import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;


@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class TntClientsTest {

    @Autowired
    private TntClients.TntClient tntClient;


    @Ignore("for manual testing that remote service is on")
    @Test
    public void manualTest() {
        Map<String, Double> pricing = tntClient.pricing("NL,DE");
        log.info("pricing={}", pricing);

        Map<String, String> track = tntClient.track("109347263,123456891");
        log.info("track={}", track);

        Map<String, List<String>> shipments = tntClient.shipments("109347263,123456891");
        log.info("shipments={}", shipments);
    }

}
