package com.gym.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

/**
 * 系统字典实体类
 * 对应数据库表: sys_dict
 */
@Data
@TableName("sys_dict")
public class SysDict implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 字典类型 (如: 'gender')
    private String dictType;

    // 字典标签 (如: '男')
    private String dictLabel;

    // 字典键值 (如: '1')
    private String dictValue;

    // 排序
    private Integer sort;

    // 是否启用? 可选，暂不添加
}
