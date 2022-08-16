package org.example.tnt.clients;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;


@Configuration
@EnableFeignClients
public class TntClients {

    @FeignClient(value = "pricing", url = "http://localhost:8080")
    public interface TntClient {

        @GetMapping("/shipments")
        Map<String, List<String>> shipments(@RequestParam(name = "q") @Valid String params);

        @GetMapping("/track")
        Map<String, String> track(@RequestParam(name = "q") @Valid String params);

        @GetMapping("/pricing")
        Map<String, Double> pricing(@RequestParam(name = "q") @Valid String params);

    }

}
