package com.gym.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单实体类修正版
 * 对应 sys_menu 表
 */
@Data
@TableName("sys_menu")
public class Menu implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String title;
    private String name;
    private String path;
    private String component;
    private String icon;
    private Long parentId;
    private Integer sort;
    private String roles;
    private Boolean hidden;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String componentStyle; // 组件样式 (glass, default, null=inherit)
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String permissionConfig; // 动作权限配置 JSON

    // 树形结构必须 (非数据库字段)
    @TableField(exist = false)
    private List<Menu> children;

    @TableField(exist = false)
    private List<String> grantedActions;
}
