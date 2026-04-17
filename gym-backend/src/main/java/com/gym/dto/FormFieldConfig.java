package com.gym.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FormFieldConfig {
    private String fieldKey;
    private String label;
    private String queryKey;
    private Boolean visible;
    private Integer order;
    private Boolean columnVisible;
    private Integer columnOrder;
    private Integer columnWidth;
    private FormFieldLayout layout;
    private Boolean filterEnabled;
    private Boolean quickSearchEnabled;
    private String controlType;
    private List<String> operatorSet;
    private String defaultOperator;
    private String defaultMatchMode;
    private Boolean allowMatchModeToggle;
    private Boolean allowMultiple;
    private String optionSourceType;
    private Map<String, Object> optionSourceConfig;
    private String placeholder;
    private Boolean hiddenButFilterable;
    private List<FormRuntimeIssue> runtimeIssues;
}
