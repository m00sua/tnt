package org.example.tnt.classes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class AggregationResponse {

    private Map<String, Double> pricing;
    private Map<String, String> tracking;
    private Map<String, List<String>> shipments;

}
