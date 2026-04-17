package com.gym.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FormPageQuickSearch {
    private Boolean enabled;
    private String placeholder;
    private List<String> fields = new ArrayList<>();
    private String defaultLogic;
}
