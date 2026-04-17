package com.gym.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FormPageMenuBinding {
    private Long menuId;
    private String menuPath;
    private String menuTitle;
    private List<String> menuTrail = new ArrayList<>();
    private String componentPath;
    private String routeName;
}
