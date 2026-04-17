package com.gym.dto;

import lombok.Data;

@Data
public class FormRuntimeIssue {
    private String code;
    private String level;
    private String message;
    private String targetKey;
    private String fieldKey;
}
