package com.org.estimator.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.estimator.ai.config.OpenAIPropertiesConfig;
import com.org.estimator.ai.service.EmbeddingService;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {
    private final WebClient webClient;
    private final OpenAIPropertiesConfig props;
    private final ObjectMapper mapper = new ObjectMapper();

    public EmbeddingServiceImpl(OpenAIPropertiesConfig props) {
        this.props = props;
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException("OpenAI API key not set");
        }
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public List<Double> createEmbedding(String text) throws Exception {
        Map<String,Object> body = Map.of(
                "input", text == null ? "" : text,
                "model", props.getModelEmbedding() == null ? "text-embedding-3-small" : props.getModelEmbedding()
        );

        Mono<String> resp = webClient.post()
                .uri("/embeddings")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30));

        String json = resp.block();
        if (json == null) throw new RuntimeException("Empty embedding response");
        JsonNode root = mapper.readTree(json);
        JsonNode data = root.path("data");
        if (!data.isArray() || data.size() == 0) throw new RuntimeException("No embeddings returned");
        JsonNode embNode = data.get(0).path("embedding");
        List<Double> emb = new ArrayList<>();
        for (JsonNode n : embNode) emb.add(n.asDouble());
        return emb;
    }
}
