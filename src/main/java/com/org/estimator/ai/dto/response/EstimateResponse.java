package com.org.estimator.ai.dto.response;

import com.org.estimator.ai.dto.request.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class EstimateResponse {
    private String projectId;
    private String projectName;
    private String summary;
    private List<EffortEstimate> effortEstimates;
    private Map<String, ResourcePlanEntry> resourcePlan;
    private List<TimelineTask> timeline;
    private List<String> assumptions;
    private List<RiskEntry> risks;
    private List<SourceInfo> sources;
    private OffsetDateTime generatedAt;

    // getters/setters
    public String getProjectId() { return projectId; } public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; } public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getSummary() { return summary; } public void setSummary(String summary) { this.summary = summary; }
    public List<EffortEstimate> getEffortEstimates() { return effortEstimates; } public void setEffortEstimates(List<EffortEstimate> effortEstimates) { this.effortEstimates = effortEstimates; }
    public Map<String, ResourcePlanEntry> getResourcePlan() { return resourcePlan; } public void setResourcePlan(Map<String, ResourcePlanEntry> resourcePlan) { this.resourcePlan = resourcePlan; }
    public List<TimelineTask> getTimeline() { return timeline; } public void setTimeline(List<TimelineTask> timeline) { this.timeline = timeline; }
    public List<String> getAssumptions() { return assumptions; } public void setAssumptions(List<String> assumptions) { this.assumptions = assumptions; }
    public List<RiskEntry> getRisks() { return risks; } public void setRisks(List<RiskEntry> risks) { this.risks = risks; }
    public List<SourceInfo> getSources() { return sources; } public void setSources(List<SourceInfo> sources) { this.sources = sources; }
    public OffsetDateTime getGeneratedAt() { return generatedAt; } public void setGeneratedAt(OffsetDateTime generatedAt) { this.generatedAt = generatedAt; }


}
