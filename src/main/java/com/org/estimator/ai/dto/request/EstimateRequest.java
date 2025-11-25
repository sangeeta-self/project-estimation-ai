package com.org.estimator.ai.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EstimateRequest {
    private String description;
    private String uploadDocId;   // existing
    private String filePath;      // NEW: optional full local path to file
    private String projectName;
    private String deadline;
    private List<String> constraints;
    private Map<String, Object> options;

}
