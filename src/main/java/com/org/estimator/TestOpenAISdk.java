package com.org.estimator;


import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;

import java.util.List;

public class TestOpenAISdk {
    public static void test() {

        int maxRetries = 5;
        long backoffMs = 1000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {

            try {
            // Build client with API key
            OpenAIClient client = OpenAIOkHttpClient.builder()
                    .apiKey("")
                    .build();

            System.out.println("Client initialized successfully!");
            ChatCompletionUserMessageParam userContent = ChatCompletionUserMessageParam.builder()
                    .content("Hello! How are you?")
                    .build();

            ChatCompletionMessageParam userMessage = ChatCompletionMessageParam.ofUser(userContent);
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model("gpt-4.1-mini")
                    .messages(List.of(userMessage))
                    .maxTokens(1024)
                    .build();

            ChatCompletion response = client.chat().completions().create(params);

            System.out.println("Client initialized successfully111!");
            System.out.println(response.choices().get(0).message().content());

            } catch (com.openai.errors.RateLimitException e) {
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    System.out.println(ie.getMessage());
                }
                backoffMs *= 2;
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

        }
    }
}
