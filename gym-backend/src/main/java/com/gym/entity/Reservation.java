package com.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("gym_reservation")
public class Reservation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long targetId;

    /**
     * Type: VENUE, EQUIPMENT, COURSE
     */
    private String targetType;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /**
     * Status: PENDING, CONFIRMED, CANCELLED, COMPLETED, CHECKED_IN
     */
    private String status;

    private LocalDateTime createdAt;
}
