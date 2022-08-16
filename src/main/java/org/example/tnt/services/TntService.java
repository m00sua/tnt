package org.example.tnt.services;

import lombok.extern.slf4j.Slf4j;
import org.example.tnt.classes.AggregationResponse;
import org.example.tnt.classes.TntExecutor;
import org.example.tnt.clients.TntClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

import static org.apache.logging.log4j.util.Strings.isNotBlank;
import static org.example.tnt.classes.TntExecutor.MILLISECONDS_IN_SECOND;
import static org.example.tnt.classes.TntExecutor.paramsToKeys;


@Slf4j
@Service
public class TntService {

    private static final int MAX_WAIT_ITERATIONS = 10;

    private TntClients.TntClient tntClient;

    private static final Object resultsArrivedMonitor = new Object();
    private static final Object resultGrabbedMonitor = new Object();

    private TntExecutor<Double> pricingExecutor;
    private TntExecutor<List<String>> shipmentsExecutor;
    private TntExecutor<String> trackExecutor;


    public TntService(@Autowired TntClients.TntClient tntClient) {
        Assert.notNull(tntClient, "TNT Client cannot be null");
        this.tntClient = tntClient;

        this.pricingExecutor = new TntExecutor<>("Pricing", 5, 5, resultsArrivedMonitor, tntClient::pricing);
        this.shipmentsExecutor = new TntExecutor<>("Shipments", 5, 5, resultsArrivedMonitor, tntClient::shipments);
        this.trackExecutor = new TntExecutor<>("Track", 5, 5, resultsArrivedMonitor, tntClient::track);
    }


    public AggregationResponse aggregate(String pricingParams, String shipmentsParams, String trackParams) {
        log.info("Aggregation starting...");
        boolean hasPricing = isNotBlank(pricingParams);
        boolean hasShipments = isNotBlank(shipmentsParams);
        boolean hasTrack = isNotBlank(trackParams);

        Assert.isTrue(hasPricing || hasShipments || hasTrack, "No pricing, shipments or track provided");

        // put keys into queue
        String[] pricingKeys = null;
        String[] shipmentsKeys = null;
        String[] trackKeys = null;

        if (hasPricing) {
            pricingKeys = paramsToKeys(pricingParams);
            pricingExecutor.addItems(pricingParams);
        }

        if (hasShipments) {
            shipmentsKeys = paramsToKeys(shipmentsParams);
            shipmentsExecutor.addItems(shipmentsParams);
        }

        if (hasTrack) {
            trackKeys = paramsToKeys(trackParams);
            trackExecutor.addItems(trackParams);
        }


        // prepare result map
        Map<String, Double> pricingResult = pricingExecutor.createResultMap(pricingKeys);
        Map<String, List<String>> shipmentsResult = shipmentsExecutor.createResultMap(shipmentsKeys);
        Map<String, String> trackResults = trackExecutor.createResultMap(trackKeys);

        // check results
        int waitIterations = MAX_WAIT_ITERATIONS;
        while (hasNoResults(pricingResult, shipmentsResult, trackResults)) {
            waitIterations--;
            if (waitIterations <= 0) {
                log.info("Break due to wait iteration limit reached");
                break;
            }

            // wait for monitor
            synchronized (resultsArrivedMonitor) {
                try {
                    log.info("Waiting for result arrived...");
                    resultsArrivedMonitor.wait(MILLISECONDS_IN_SECOND);
                } catch (InterruptedException e) {
                    log.error("Waiting was interrupted");
                }
            }

            // get results
            log.info("Getting results...");
            synchronized (resultGrabbedMonitor) {
                pricingExecutor.grabResults(pricingResult);
                shipmentsExecutor.grabResults(shipmentsResult);
                trackExecutor.grabResults(trackResults);
            }
        } // of while

        log.info("Sending the results to client");

        return new AggregationResponse(pricingResult, trackResults, shipmentsResult);
    }


    private boolean hasNoResults(Map<String, Double> pricingResult,
                                 Map<String, List<String>> shipmentsResult,
                                 Map<String, String> trackResults) {
        return hasEmptyValue(pricingResult) || hasEmptyValue(shipmentsResult) || hasEmptyValue(trackResults);
    }


    private static <K, V> boolean hasEmptyValue(Map<K, V> map) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                return true;
            }
        }
        return false;
    }

}
