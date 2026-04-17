package com.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("gym_member_private_package")
public class MemberPrivatePackage implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long coachId;

    private Long packageId;

    private String packageName;

    private Integer totalSessions;

    private Integer remainingSessions;

    private String status;

    private LocalDateTime createdAt;
}
