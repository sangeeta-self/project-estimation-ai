package com.org.estimator.ai.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.estimator.ai.service.VectorDbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class VectorDbServiceImpl implements VectorDbService {

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String indexName;
    private final String pineconeBase;

    public VectorDbServiceImpl(@Value("${pinecone.api.key}") String pineconeApiKey,
                               @Value("${pinecone.environment}") String pineconeEnv,
                               @Value("${pinecone.index.name}") String indexName) {
        this.indexName = indexName;
        if (pineconeApiKey == null || pineconeApiKey.isBlank()) {
            throw new IllegalStateException("Pinecone API key not set");
        }
        // Pinecone REST base url: https://controller.<env>.pinecone.io or index url: https://<index>-<project>.svc.<env>.pinecone.io
        // We'll call the upsert endpoint using index-specific URL pattern:
        this.pineconeBase = String.format("https://%s-%s.svc.%s.pinecone.io", indexName, "default", pineconeEnv);
        this.webClient = WebClient.builder()
                .baseUrl(pineconeBase)
                .defaultHeader("Api-Key", pineconeApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public void upsertBatch(List<Map<String, Object>> items, String namespace) throws Exception {
        // items: list of { id, values (List<Double>), metadata(Map) }
        Map<String,Object> body = Map.of("vectors", items);
        if (namespace != null && !namespace.isBlank()) {
            body = Map.of("vectors", items, "namespace", namespace);
        }
        Mono<String> resp = webClient.post()
                .uri("/vectors/upsert")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30));
        String json = resp.block();
        if (json == null) throw new RuntimeException("Empty upsert response from Pinecone");
        // optional: parse response for errors
    }



    @Override
    public void upsertVector(String id, List<Double> vector, Map<String, Object> metadata, String namespace) throws Exception {
        Map<String,Object> vec = Map.of("id", id, "values", vector, "metadata", metadata);
        upsertBatch(List.of(vec), namespace);
    }

}
