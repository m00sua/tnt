package org.example.tnt.controllers;

import org.example.tnt.classes.AggregationResponse;
import org.example.tnt.services.TntService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/aggregation")
public class TntController {

    @Autowired
    private TntService tntService;


    @GetMapping
    public AggregationResponse aggregation(
            @RequestParam(required = false, name = "pricing") String pricingParams,
            @RequestParam(required = false, name = "shipments") String shipmentsParams,
            @RequestParam(required = false, name = "track") String trackParams) {
        return tntService.aggregate(pricingParams, shipmentsParams, trackParams);
    }

}
