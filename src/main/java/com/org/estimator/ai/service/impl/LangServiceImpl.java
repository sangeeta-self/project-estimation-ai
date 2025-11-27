package com.org.estimator.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.estimator.ai.config.OpenAIProperties;
import com.org.estimator.ai.service.LangService;
import com.org.estimator.ai.service.lang.PromptLoaderService;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class LangServiceImpl implements LangService {

    private final PromptLoaderService prompts;
    private final WebClient webClient;
    private final OpenAIProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    public LangServiceImpl(PromptLoaderService prompts, OpenAIProperties props) {
        this.prompts = prompts;
        this.props = props;
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException("OpenAI API key not set (openai.api.key)");
        }
        this.webClient = WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

    }

    @Override
    public String breakTasksPrompt(String description) {
        return prompts.render("tasks_division.prompt", Map.of("DESCRIPTION", description == null ? "" : description));
    }

    @Override
    public String estimateEffortPrompt(String tasksJson, String optionsJson) {
        return prompts.render("effort_estimate.prompt", Map.of("TASKS_JSON", tasksJson == null ? "" : tasksJson,
                "OPTIONS", optionsJson == null ? "" : optionsJson));
    }

    @Override
    public String resourcePlanPrompt(String estimatesJson, String constraints, String deadline) {
        return prompts.render("resource_plan.prompt", Map.of("ESTIMATES_JSON", estimatesJson == null ? "" : estimatesJson,
                "CONSTRAINTS", constraints == null ? "" : constraints,
                "DEADLINE", deadline == null ? "" : deadline));
    }

    @Override
    public String assumptionsPrompt(String summary) {
        return prompts.render("assumptions_risks.prompt", Map.of("PROJECT_SUMMARY", summary == null ? "" : summary));
    }

    @Override
    public String callChat(String prompt) throws Exception {
        System.out.println("******************************");
        System.out.println("API_key "+props.getApiKey());
        System.out.println("Base Url "+props.getBaseUrl());

        Map<String,Object> body = new HashMap<>();
        body.put("model", props.getModelChat() == null ? "gpt-4o-mini" : props.getModelChat());
        body.put("messages", new Object[] { Map.of("role", "user", "content", prompt) });
        body.put("temperature", props.getTemperature() == null ? 0.2 : props.getTemperature());

        Mono<String> resp = webClient.post()
                .uri("/chat/completions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(60));

        String json = resp.block();
        if (json == null) throw new RuntimeException("Empty response from OpenAI");
        JsonNode root = mapper.readTree(json);
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            JsonNode message = choices.get(0).path("message");
            String content = message.path("content").asText(null);
            if (content != null) return content.trim();
        }
        String text = root.path("text").asText(null);
        if (text != null) return text.trim();
        throw new RuntimeException("No assistant content returned from OpenAI: " + json);
    }

}
