package org.example.tnt.services;

import lombok.extern.slf4j.Slf4j;
import org.example.tnt.classes.AggregationResponse;
import org.example.tnt.clients.TntClients;
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

        Map<String, List<String>> shipments = null;
        if (hasShipments) {
            try {
                shipments = tntClient.shipments(shipmentsParams);
            } catch (Exception e) {
                log.error("Cannot get Shipments due to {} : {}", e.getClass().getSimpleName(), e.getMessage());
            }
        }


        Map<String, Double> pricing = null;
        if (hasPricing) {
            try {
                pricing = tntClient.pricing(pricingParams);
            } catch (Exception e) {
                log.error("Cannot get Pricing due to {} : {}", e.getClass().getSimpleName(), e.getMessage());
            }
        }


        Map<String, String> track = null;
        if (hasTrack) {
            try {
                track = tntClient.track(trackParams);
            } catch (Exception e) {
                log.error("Cannot get Track due to {} : {}", e.getClass().getSimpleName(), e.getMessage());
            }
        }

        return new AggregationResponse(pricing, track, shipments);
    }

}
