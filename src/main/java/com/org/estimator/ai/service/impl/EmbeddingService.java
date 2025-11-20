package com.org.estimator.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.estimator.ai.config.AppConfig;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {
    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final AppConfig config;

    public EmbeddingService(AppConfig config) { this.config = config; }

    // returns List<Double> embedding vector
    public List<Double> createEmbedding(String text) throws Exception {
        String apiKey = config.getOpenAiApiKey();
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("OPENAI_API_KEY not set");
        String model = config.getOpenAiEmbeddingModel();
        String url = "https://api.openai.com/v1/embeddings";

        var payload = mapper.createObjectNode();
        payload.put("model", model);
        payload.put("input", text);

        RequestBody body = RequestBody.create(payload.toString(), MediaType.get("application/json"));
        Request req = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) throw new RuntimeException("Embedding failed: " + resp.code() + " " + resp.body().string());
            JsonNode root = mapper.readTree(resp.body().string());
            JsonNode emb = root.get("data").get(0).get("embedding");
            List<Double> vector = new ArrayList<>();
            for (JsonNode n : emb) vector.add(n.asDouble());
            return vector;
        }
    }
}
