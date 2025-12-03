package com.org.estimator.ai.service.impl;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import com.org.estimator.ai.config.OpenAIPropertiesConfig;
import com.org.estimator.ai.service.LangService;
import com.org.estimator.ai.service.lang.PromptLoaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LangServiceImpl implements LangService {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final PromptLoaderService promptLoader;
    private final OpenAIPropertiesConfig props;
    private final OpenAIClient client;

    public LangServiceImpl(PromptLoaderService promptLoader, OpenAIPropertiesConfig props) {
        this.promptLoader = promptLoader;
        this.props = props;

        String apiKey = props.getApiKey();
        if (apiKey == null || apiKey.isBlank()) apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key not provided. Set openai.api-key or OPENAI_API_KEY.");
        }

        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
        log.info("LangServiceImpl initialized (model-chat={}, baseUrl={})",
                props.getModelChat(), props.getBaseUrl());
    }



    @Override
    public String breakTasksPrompt(String description) {
        return promptLoader.render("tasks_division.prompt", Map.of("DESCRIPTION", description == null ? "" : description));
    }

    @Override
    public String estimateEffortPrompt(String tasksJson, String optionsJson) {
        return promptLoader.render("effort_estimate.prompt", Map.of("TASKS_JSON", tasksJson == null ? "" : tasksJson,
                "OPTIONS", optionsJson == null ? "" : optionsJson));
    }

    @Override
    public String resourcePlanPrompt(String estimatesJson, String constraints, String deadline) {
        return promptLoader.render("resource_plan.prompt", Map.of("ESTIMATES_JSON", estimatesJson == null ? "" : estimatesJson,
                "CONSTRAINTS", constraints == null ? "" : constraints,
                "DEADLINE", deadline == null ? "" : deadline));
    }

    @Override
    public String assumptionsPrompt(String summary) {
        return promptLoader.render("assumptions_risks.prompt", Map.of("PROJECT_SUMMARY", summary == null ? "" : summary));
    }


    @Override
    public String callChat(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }
        int maxRetries = 5;
        long backoffMs = 1000;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Build user message
                ChatCompletionUserMessageParam userContent = ChatCompletionUserMessageParam.builder()
                        .content(prompt)
                        .build();

                ChatCompletionMessageParam userMessage = ChatCompletionMessageParam.ofUser(userContent);

                // Build request
                ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                        .model(props.getModelChat())
                        .messages(List.of(userMessage))
                        .maxTokens(1024)
                        .build();

                // Call API
                ChatCompletion response = client.chat().completions().create(params);
                String content = response.choices().get(0).message().content().orElse("");
                response = null;
                return content;

            } catch (com.openai.errors.RateLimitException e) {
                    log.warn("Rate limit hit (attempt {} of {}), retrying in {} ms...", attempt, maxRetries, backoffMs);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return "Error: Interrupted during retry";
                    }
                    backoffMs *= 2;
            } catch (Exception e) {
                    log.error("Error calling OpenAI chat API", e);
                    return "Error: " + e.getMessage();
            }

        }
        return "Error: Failed after " + maxRetries + " attempts due to rate limiting.";
    }


}
