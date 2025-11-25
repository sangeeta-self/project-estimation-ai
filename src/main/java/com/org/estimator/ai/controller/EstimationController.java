package com.org.estimator.ai.controller;

import com.org.estimator.ai.dto.request.EstimateRequest;
import com.org.estimator.ai.dto.response.EstimateResponse;
import com.org.estimator.ai.service.EstimationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/estimations")
@Tag(name = "Estimation", description = "Generate AI-driven project estimates")
public class EstimationController {

    private final EstimationService estimationService;

    public EstimationController(EstimationService estimationService) {
        this.estimationService = estimationService;
    }

    @Operation(summary = "Generate unified project estimate")
    @PostMapping(value = "/get-estimate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EstimateResponse> estimate(@RequestBody EstimateRequest request) throws Exception {
        EstimateResponse resp = estimationService.estimate(request);
        return ResponseEntity.ok(resp);
    }
}
