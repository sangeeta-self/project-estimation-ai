package com.org.estimator.ai.service;

import java.util.List;
import java.util.Map;

public interface VectorDbService {
    void upsertBatch(List<Map<String,Object>> items, String namespace) throws Exception;
    void upsertVector(String id, List<Double> vector, Map<String,Object> metadata, String namespace) throws Exception;
}
