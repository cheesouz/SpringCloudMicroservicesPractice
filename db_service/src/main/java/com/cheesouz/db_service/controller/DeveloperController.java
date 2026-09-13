package com.cheesouz.db_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/dev")
public class DeveloperController {
    @GetMapping("/chaos")
    public ResponseEntity<String> chaos(@RequestParam int delayMs, @RequestParam double errorRate) {

        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Interrupted while sleeping");
            }
        }

        if (errorRate > 0) {
            int randomValue = (int) (Math.random() * 100);
            if (randomValue < errorRate) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Simulated error");
            }
        }

        return ResponseEntity.ok("Response from chaos endpoint with delay: " + delayMs + " ms and error rate: " + errorRate + "%");
    }
}
