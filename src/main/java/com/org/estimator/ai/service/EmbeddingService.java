package com.org.estimator.ai.service;

import java.util.List;

public interface EmbeddingService {

    List<Double> createEmbedding(String text) throws Exception;
}
