package com.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("gym_body_metric")
public class BodyMetric implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long coachId;

    private BigDecimal height;

    private BigDecimal weight;

    private BigDecimal bodyFat;

    private BigDecimal bmi;

    private String remark;

    private LocalDateTime measuredAt;
}
