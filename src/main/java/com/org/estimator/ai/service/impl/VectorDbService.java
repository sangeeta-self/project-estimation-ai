package com.org.estimator.ai.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.estimator.ai.config.AppConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VectorDbService {

    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final AppConfig config;

    public VectorDbService(AppConfig config) { this.config = config; }

    public void upsertVector(String id, List<Double> vector, String chunkText, String docId) throws Exception {
        String apiKey = config.getPineconeApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("Pinecone not configured; skipping upsert for " + id);
            return;
        }
        String env = config.getPineconeEnvironment();
        String index = config.getPineconeIndexName();

        String url = String.format("https://%s-%s.svc.%s.pinecone.io/vectors/upsert", index, index, env);

        Map<String,Object> payload = new HashMap<>();
        Map<String,Object> vec = new HashMap<>();
        vec.put("id", id);
        vec.put("values", vector);
        Map<String,String> metadata = new HashMap<>();
        metadata.put("docId", docId);
        metadata.put("text", chunkText.length() > 200 ? chunkText.substring(0,200) : chunkText);
        vec.put("metadata", metadata);
        payload.put("vectors", List.of(vec));

        RequestBody body = RequestBody.create(mapper.writeValueAsString(payload), MediaType.get("application/json"));
        Request req = new Request.Builder()
                .url(url)
                .addHeader("Api-Key", apiKey)
                .post(body)
                .build();

        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                System.err.println("Pinecone upsert failed: " + resp.code() + " " + (resp.body() == null ? "" : resp.body().string()));
            } else {
                System.out.println("Upsert succeeded for " + id);
            }
        }
    }
}
