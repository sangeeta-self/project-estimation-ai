package com.org.estimator.ai.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "openai")
public class OpenAIPropertiesConfig {

    private String apiKey;
    private String modelChat ;  // default (can override)
    private Double temperature ;
    private String modelEmbedding;
    private String baseUrl;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModelChat() { return modelChat; }
    public void setModelChat(String modelChat) { this.modelChat = modelChat; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public String getModelEmbedding() { return modelEmbedding; }
    public void setModelEmbedding(String modelEmbedding) { this.modelEmbedding = modelEmbedding; }


    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

}
