package com.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String realName;

    /**
     * Role: ADMIN, STAFF, MEMBER
     */
    private String role;

    private String phone;

    private String avatar;

    private BigDecimal balance;

    /**
     * Member Type: REGULAR, VIP
     */
    private String type;

    private LocalDateTime createdAt;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String token;
}
