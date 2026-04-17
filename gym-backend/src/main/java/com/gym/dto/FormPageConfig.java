package com.gym.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FormPageConfig {
    private Integer version;
    private String pageKey;
    private String routePath;
    private String pageTitle;
    private Boolean enabled;
    private Boolean formInputFollowSystemRadius;
    private FormPageMenuBinding menuBinding;
    private FormPageQuickSearch quickSearch;
    private List<FormConfigTarget> targets = new ArrayList<>();
    private String quickSearchPlaceholder;
    private List<String> quickSearchFields = new ArrayList<>();
    private String defaultFilterLogic;
    private List<FormFieldConfig> fields = new ArrayList<>();
    private List<FormRuntimeIssue> runtimeIssues = new ArrayList<>();
}
