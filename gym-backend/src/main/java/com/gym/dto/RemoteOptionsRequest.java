package com.gym.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class RemoteOptionsRequest {
    private String pageKey;
    private String routePath;
    private String keyword;
    private Integer limit = 20;
    private String optionSourceType;
    private Map<String, Object> optionSourceConfig = new HashMap<>();
}
