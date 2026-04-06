package com.gym.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gym.entity.Coach;

/**
 * 教练服务接口
 * 继承 IService 以获得 MyBatis-Plus 提供的通用业务逻辑方法
 * 如: save, removeById, getById, page 等
 */
public interface CoachService extends IService<Coach> {
    // 可以在此定义额外的业务方法接口
}
