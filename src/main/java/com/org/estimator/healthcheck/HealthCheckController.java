package com.org.estimator.healthcheck;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1")
@Tag(name = "Estimation", description = "Generate AI-driven project estimates")
public class HealthCheckController {

    public HealthCheckController() {
    }

    @Operation(summary = "Health Check")
    @GetMapping("/healthcheck")
    public ResponseEntity<String> healthcheck() {
        return ResponseEntity.ok("The Project Estimation AI api is up and running. Time Now: " + LocalDateTime.now());
    }
}
