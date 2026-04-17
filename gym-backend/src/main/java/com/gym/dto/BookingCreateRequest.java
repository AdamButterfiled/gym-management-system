package com.gym.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingCreateRequest {
    private String resourceType;
    private Long resourceId;
    private String source;
    private String idempotentKey;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
