package com.cheesouz.api_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cheesouz.api_service.client.ApiClient;

@RestController
@RequestMapping("/api/dev")
public class DeveloperController {
    private final ApiClient userClient;
    
    public DeveloperController(ApiClient userClient) {
        this.userClient = userClient;
    }

    @GetMapping("/chaos")
    public ResponseEntity<String> chaos(@RequestParam(defaultValue = "5000") int delayMs, @RequestParam(defaultValue = "0.5") double errorRate){
        return userClient.chaos(delayMs, errorRate);
    }
    
}
