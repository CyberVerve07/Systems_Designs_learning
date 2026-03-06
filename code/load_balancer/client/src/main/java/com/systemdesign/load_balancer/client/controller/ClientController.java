package com.systemdesign.load_balancer.client.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

@RestController
public class ClientController {

    @Autowired
    private RestTemplate restTemplate;

    // This simulates a client sending a request. 
    // In a real system design, the URL would be the Load Balancer's IP address.
    @GetMapping("/send-request")
    public String sendRequestToLoadBalancer(@RequestParam(defaultValue = "http://localhost:8080") String lbUrl) {
        
        System.out.println("Client is sending request to Load Balancer at: " + lbUrl);
        
        try {
            // Making a GET request to the Load Balancer
            ResponseEntity<String> response = restTemplate.getForEntity(lbUrl + "/api/data", String.class);
            return "Response from Server (via Load Balancer): " + response.getBody();
        } catch (Exception e) {
            return "Failed to connect to Load Balancer: " + e.getMessage() + "\nMake sure the Load Balancer/Server is actually running at " + lbUrl;
        }
    }
}
