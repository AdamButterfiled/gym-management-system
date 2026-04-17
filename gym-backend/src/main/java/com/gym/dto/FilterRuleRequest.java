package com.gym.dto;

import lombok.Data;

@Data
public class FilterRuleRequest {
    private String fieldKey;
    private String queryKey;
    private String controlType;
    private String operator;
    private String matchMode;
    private Object value;
    private Object valueTo;
}
