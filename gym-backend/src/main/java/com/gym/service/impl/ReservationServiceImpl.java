package com.gym.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.entity.Reservation;
import com.gym.mapper.ReservationMapper;
import com.gym.service.ReservationService;
import org.springframework.stereotype.Service;

@Service
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper, Reservation> implements ReservationService {
}
