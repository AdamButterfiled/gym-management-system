package com.gym.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentCreateRequest {
    private String paymentType;
    private Long targetId;
    private BigDecimal amount;
}
