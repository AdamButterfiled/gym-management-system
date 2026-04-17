package com.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 教练实体类
 * 对应数据库表: gym_coach
 */
@Data
@TableName("gym_coach")
public class Coach implements Serializable {

    // 序列化ID，用于确保序列化兼容性
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     * IdType.AUTO 表示数据库自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    /**
     * 教练姓名
     */
    private String name;

    /**
     * 性别
     * 1: 男, 0: 女
     */
    private Integer gender;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 擅长项目 / 专长
     * 例如: "瑜伽", "减脂", "增肌"
     */
    private String specialization;

    /**
     * 入职日期
     * 使用 LocalDate 类型
     */
    private LocalDate entryDate;

    /**
     * 个人简介 / 描述
     */
    private String intro;

    /**
     * 状态
     * 1: 在职 (Active)
     * 0: 离职 (Resigned)
     */
    private Integer status;

    private String avatar;

    private BigDecimal hourlyPrice;

    private BigDecimal rating;
}
