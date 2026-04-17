package com.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("gym_private_schedule")
public class PrivateSchedule implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long coachId;

    private Long venueId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer capacity;

    private Integer bookedCount;

    private String description;

    private String status;

    private LocalDateTime createdAt;
}
