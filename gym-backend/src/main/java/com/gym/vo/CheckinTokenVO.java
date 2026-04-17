package com.gym.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CheckinTokenVO {
    private Long bookingId;
    private String token;
    private LocalDateTime expireTime;
}
