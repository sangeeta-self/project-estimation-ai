package com.org.estimator.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.estimator.ai.config.AppConfig;
import com.org.estimator.ai.service.LangService;
import okhttp3.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LangServiceImpl implements LangService {

    private final AppConfig config;
    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private String breakTasksPrompt;
    private String estimateEffortPrompt;
    private String resourcePlanPrompt;
    private String assumptionsPrompt;

    public LangServiceImpl(AppConfig config) {
        this.config = config;
        loadPrompts();
    }

    private void loadPrompts() {
        try {
            breakTasksPrompt = new String(new ClassPathResource("prompt-templates/break_tasks.prompt").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            estimateEffortPrompt = new String(new ClassPathResource("prompt-templates/estimate_effort.prompt").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            resourcePlanPrompt = new String(new ClassPathResource("prompt-templates/resource_plan.prompt").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assumptionsPrompt = new String(new ClassPathResource("prompt-templates/assumptions_risks.prompt").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load prompts", e);
        }
    }

    @Override
    public String breakTasksPrompt(String requirements) {
        return breakTasksPrompt.replace("{requirements}", escape(requirements));
    }

    @Override
    public String estimateEffortPrompt(String tasksJson, String context) {
        return estimateEffortPrompt.replace("{tasks}", escape(tasksJson)).replace("{context}", escape(context == null ? "" : context));
    }

    @Override
    public String resourcePlanPrompt(String estimatesJson, String constraints, String deadline) {
        return resourcePlanPrompt.replace("{estimates}", escape(estimatesJson))
                .replace("{constraints}", escape(constraints == null ? "" : constraints))
                .replace("{deadline}", escape(deadline == null ? "" : deadline));
    }

    @Override
    public String assumptionsPrompt(String summary) {
        return assumptionsPrompt.replace("{summary}", escape(summary));
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("`","").replace("$","");
    }

    @Override
    public String callChat(String prompt) throws Exception {
        String url = "https://api.openai.com/v1/chat/completions";
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", config.getOpenAiChatModel());
        var messages = List.of(
                Map.of("role","system","content","You are a senior project estimator. Return strict JSON only."),
                Map.of("role","user","content", prompt)
        );
        payload.put("messages", messages);
        payload.put("temperature", 0.2);

        RequestBody body = RequestBody.create(mapper.writeValueAsString(payload), MediaType.get("application/json"));
        Request req = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + config.getOpenAiApiKey())
                .post(body)
                .build();

        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) throw new RuntimeException("OpenAI chat failed: " + resp.code() + " " + resp.body().string());
            JsonNode root = mapper.readTree(resp.body().string());
            JsonNode choices = root.get("choices");
            if (choices != null && choices.size() > 0) {
                return choices.get(0).get("message").get("content").asText();
            }
            return "";
        }
    }

}
