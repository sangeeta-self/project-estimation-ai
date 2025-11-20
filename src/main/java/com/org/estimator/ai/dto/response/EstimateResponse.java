package com.org.estimator.ai.dto.response;

import com.org.estimator.ai.dto.request.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
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

}
