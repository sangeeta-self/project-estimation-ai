package com.org.estimator.ai.service;

public interface LangService {
    String breakTasksPrompt(String requirements);
    String estimateEffortPrompt(String tasksJson, String context);
    String resourcePlanPrompt(String estimatesJson, String constraints, String deadline);
    String assumptionsPrompt(String summary);
    String callChat(String prompt) throws Exception;
}
