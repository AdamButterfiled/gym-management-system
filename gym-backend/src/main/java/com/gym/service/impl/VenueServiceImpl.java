package com.gym.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.entity.Venue;
import com.gym.mapper.VenueMapper;
import com.gym.service.VenueService;
import org.springframework.stereotype.Service;

@Service
public class VenueServiceImpl extends ServiceImpl<VenueMapper, Venue> implements VenueService {
}
