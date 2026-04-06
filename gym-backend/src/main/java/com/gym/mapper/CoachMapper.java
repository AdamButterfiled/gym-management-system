package com.gym.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.entity.Coach;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教练 Mapper 接口
 * 继承 MyBatis-Plus 的 BaseMapper，自动拥有基础 CRUD 功能
 * 无需编写 XML 文件即可使用
 */
@Mapper
public interface CoachMapper extends BaseMapper<Coach> {
    // 如果有复杂的自定义 SQL，可以在这里定义方法并在 XML 中实现
    // 目前基础功能不需要
}
