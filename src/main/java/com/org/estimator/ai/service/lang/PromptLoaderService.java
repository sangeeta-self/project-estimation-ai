package com.org.estimator.ai.service.lang;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class PromptLoaderService {

    public String render(String promptFileName, Map<String, String> variables) {
        try {
            String content = loadPromptFile(promptFileName);

            if (variables != null) {
                for (var entry : variables.entrySet()) {
                    String placeholder = "<<<" + entry.getKey() + ">>>";
                    content = content.replace(placeholder, entry.getValue() == null ? "" : entry.getValue());
                }
            }

            return content;

        } catch (Exception ex) {
            throw new RuntimeException("Failed to render prompt file: " + promptFileName, ex);
        }
    }


    /**
     * Reads a prompt file from classpath resources.
     */
    private String loadPromptFile(String fileName) throws IOException {
        ClassPathResource resource = new ClassPathResource("/prompts/" + fileName);

        if (!resource.exists()) {
            throw new IOException("Prompt file not found: prompts/" + fileName);
        }

        byte[] bytes = resource.getInputStream().readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
