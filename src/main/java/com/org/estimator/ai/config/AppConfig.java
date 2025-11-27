package com.org.estimator.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Value("$x{openai.api-key:}")
    private String openAiApiKey;
    @Value("${openai.model-chat:gpt-4o-mini}")
    private String openAiChatModel;
    @Value("${openai.model-embedding:text-embedding-3-small}")
    private String openAiEmbeddingModel;
    @Value("${pinecone.api-key:}")
    private String pineconeApiKey;
    @Value("${pinecone.environment:}")
    private String pineconeEnvironment;
    @Value("${pinecone.index-name:project-estimator}")
    private String pineconeIndexName;


    @Value("${ocr.enabled:false}")
    private boolean ocrEnabled;
    @Value("${ocr.tessdata.path:}")
    private String tessdataPath;



    public String getOpenAiApiKey() { return openAiApiKey; }
    public String getOpenAiChatModel() { return openAiChatModel; }
    public String getOpenAiEmbeddingModel() { return openAiEmbeddingModel; }
    public String getPineconeApiKey() { return pineconeApiKey; }
    public String getPineconeEnvironment() { return pineconeEnvironment; }
    public String getPineconeIndexName() { return pineconeIndexName; }

}
