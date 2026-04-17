package com.gym.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TableQueryRequest {
    private String pageKey;
    private String routePath;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String keyword;
    private String filterLogic = "AND";
    private List<FilterRuleRequest> filterRules = new ArrayList<>();
}
