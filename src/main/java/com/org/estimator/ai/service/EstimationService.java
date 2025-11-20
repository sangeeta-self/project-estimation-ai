package com.org.estimator.ai.service;

import com.org.estimator.ai.dto.request.EstimateRequest;
import com.org.estimator.ai.dto.response.EstimateResponse;

public interface EstimationService {

    EstimateResponse estimate(EstimateRequest req) throws Exception;
}
