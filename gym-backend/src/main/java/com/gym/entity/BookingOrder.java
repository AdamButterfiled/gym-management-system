package com.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("gym_booking_order")
public class BookingOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private String resourceType;

    private Long resourceId;

    private String resourceName;

    private Long venueId;

    private Long coachId;

    private LocalDate bookingDate;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private BigDecimal amount;

    private String paymentStatus;

    private String status;

    private String source;

    private String idempotentKey;

    private String qrToken;

    private LocalDateTime qrExpireTime;

    private LocalDateTime checkedInAt;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
