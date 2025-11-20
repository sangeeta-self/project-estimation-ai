package com.org.estimator.ai.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResourcePlanEntry {

    private Integer effortHours;
    private Integer resourcesNeeded;
    private Integer durationWeeks;
}
