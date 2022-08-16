package org.example.tnt.services;

import lombok.extern.slf4j.Slf4j;
import org.example.tnt.classes.AggregationResponse;
import org.example.tnt.clients.TntClients;
import org.example.tnt.clients.Wrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

import static org.apache.logging.log4j.util.Strings.isNotBlank;


@Slf4j
@Service
public class TntService {

    private TntClients.TntClient tntClient;


    public TntService(@Autowired TntClients.TntClient tntClient) {
        Assert.notNull(tntClient, "TNT Client cannot be null");
        this.tntClient = tntClient;
    }


    public AggregationResponse aggregate(String pricingParams, String shipmentsParams, String trackParams) {
        log.info("Aggregation starting...");
        boolean hasPricing = isNotBlank(pricingParams);
        boolean hasShipments = isNotBlank(shipmentsParams);
        boolean hasTrack = isNotBlank(trackParams);

        Assert.isTrue(hasPricing || hasShipments || hasTrack, "No pricing, shipments or track provided");


        Wrapper<Map<String, List<String>>> wrapperShipments = new Wrapper<>();
        Wrapper<Map<String, Double>> wrapperPricing = new Wrapper<>();
        Wrapper<Map<String, String>> wrapperTrack = new Wrapper<>();

        Thread threadShipments = null;
        Thread threadPricing = null;
        Thread threadTrack = null;

        if (hasShipments) {
            threadShipments = runInThread("Shipments", () -> {
                wrapperShipments.setObject(tntClient.shipments(shipmentsParams));
            });
        }

        if (hasPricing) {
            threadPricing = runInThread("Pricing", () -> {
                wrapperPricing.setObject(tntClient.pricing(pricingParams));
            });
        }

        if (hasTrack) {
            threadTrack = runInThread("Truck", () -> {
                wrapperTrack.setObject(tntClient.track(trackParams));
            });
        }

        waitForThreads(threadPricing, threadShipments, threadTrack);
        return new AggregationResponse(wrapperPricing.getObject(), wrapperTrack.getObject(), wrapperShipments.getObject());
    }


    private Thread runInThread(String prefix, Runnable run) {
        Assert.hasText(prefix, "Prefix cannot be blank");
        Assert.notNull(run, "Runnable cannot be null");
        
        Thread thread = new Thread(() -> {
            try {
                log.info("Requesting {}...", prefix);
                run.run();
                log.info("{} done", prefix);
            } catch (Exception e) {
                log.error("Cannot get {} due to {} : {}", prefix, e.getClass().getSimpleName(), e.getMessage());
            }
        });
        thread.setName(prefix);
        thread.start();
        return thread;
    }


    private void waitForThreads(Thread... threads) {
        if (threads == null || threads.length <= 0) {
            return;
        }

        for (Thread thread : threads) {
            if (thread != null) {
                try {
                    log.info("Waiting for {}...", thread.getName());
                    thread.join();
                    log.info("Waiting for {} is done", thread.getName());
                } catch (InterruptedException e) {
                    log.error("Thread interrupted");
                }
            }
        }
    }

}
