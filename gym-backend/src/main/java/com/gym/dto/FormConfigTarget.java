package com.gym.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FormConfigTarget {
    private String targetKey;
    private String targetType;
    private String title;
    private Boolean enabled;
    private Integer order;
    private FormConfigTargetSourceSignature sourceSignature;
    private List<FormFieldConfig> fields = new ArrayList<>();
    private List<FormRuntimeIssue> runtimeIssues = new ArrayList<>();
}
