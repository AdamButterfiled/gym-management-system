package com.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalTime;

@Data
@TableName("gym_venue")
public class Venue implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private String location;

    private Integer capacity;

    private String description;

    /**
     * Status: 1-Open, 0-Closed
     */
    private Integer status;

    private LocalTime openTime;

    private LocalTime closeTime;

    private String image;
}
