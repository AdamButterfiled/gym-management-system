package com.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_form_config")
public class FormConfigPage implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String pageKey;
    private String routePath;
    private String pageTitle;
    private Boolean enabled;
    private String configJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
