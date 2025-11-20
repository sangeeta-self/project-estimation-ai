package com.org.estimator.ai.service;

import java.util.List;

public interface VectorDbService {

    void upsertVector(String id, List<Double> vector, String chunkText, String docId) throws Exception ;
}
