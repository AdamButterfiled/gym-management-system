package com.gym.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.entity.Repair;
import com.gym.mapper.RepairMapper;
import com.gym.service.RepairService;
import org.springframework.stereotype.Service;

@Service
public class RepairServiceImpl extends ServiceImpl<RepairMapper, Repair> implements RepairService {
}
