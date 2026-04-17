package com.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("gym_resource_slot")
public class ResourceSlot implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String resourceType;

    private Long resourceId;

    private Long venueId;

    private Long coachId;

    private LocalDate slotDate;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer capacity;

    private Integer occupiedCount;

    private Integer version;

    private String status;

    private LocalDateTime createdAt;
}
