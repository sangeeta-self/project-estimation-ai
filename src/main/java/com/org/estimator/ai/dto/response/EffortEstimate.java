package com.org.estimator.ai.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EffortEstimate {

    private String module;
    private String feature;
    private String task;
    private String role;
    private Integer hours;
    private Double confidence;
}
