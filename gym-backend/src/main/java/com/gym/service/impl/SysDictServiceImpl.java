package com.gym.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.entity.SysDict;
import com.gym.mapper.SysDictMapper;
import com.gym.service.SysDictService;
import org.springframework.stereotype.Service;

@Service
public class SysDictServiceImpl extends ServiceImpl<SysDictMapper, SysDict> implements SysDictService {
}
