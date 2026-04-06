package com.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("gym_repair")
public class Repair implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long equipmentId;
    private Long venueId;
    private Long reporterId;
    private String description;
    private String status; // PENDING, PROCESSING, FIXED
    private LocalDateTime createdAt;
}
