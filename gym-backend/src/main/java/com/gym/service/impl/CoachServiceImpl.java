package com.gym.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.entity.Coach;
import com.gym.mapper.CoachMapper;
import com.gym.service.CoachService;
import org.springframework.stereotype.Service;

/**
 * 教练服务实现类
 * 继承 ServiceImpl 实现通用逻辑
 * 实现 CoachService 接口
 */
@Service // 标记为 Spring 的服务组件
public class CoachServiceImpl extends ServiceImpl<CoachMapper, Coach> implements CoachService {
    // 这里可以直接使用 baseMapper 进行数据库操作
    // 也可以复写父类方法增加业务逻辑
}
