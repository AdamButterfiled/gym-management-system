package com.gym.dto;

import lombok.Data;

@Data
public class FormConfigTargetSourceSignature {
    private String componentPath;
    private String routePath;
    private String routeName;
    private String columnsBinding;
    private String baseColumnsBinding;
    private String modalBinding;
    private String modalTitleBinding;
    private String titleCandidate;
    private String detectedBy;
}
