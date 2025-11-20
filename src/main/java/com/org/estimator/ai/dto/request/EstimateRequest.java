package com.org.estimator.ai.dto.request;

import java.util.List;
import java.util.Map;

public class EstimateRequest {
    private String uploadDocId;
    private String projectName;
    private String description;
    private String deadline;
    private List<String> techStack;
    private List<String> constraints;
    private Map<String,Object> options;

    public String getUploadDocId() { return uploadDocId; }
    public void setUploadDocId(String uploadDocId) { this.uploadDocId = uploadDocId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
    public List<String> getTechStack() { return techStack; }
    public void setTechStack(List<String> techStack) { this.techStack = techStack; }
    public List<String> getConstraints() { return constraints; }
    public void setConstraints(List<String> constraints) { this.constraints = constraints; }
    public Map<String, Object> getOptions() { return options; }
    public void setOptions(Map<String, Object> options) { this.options = options; }
}
