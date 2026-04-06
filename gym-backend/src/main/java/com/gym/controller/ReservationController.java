package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.common.Result;
import com.gym.entity.Reservation;
import com.gym.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservation")
@CrossOrigin
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @GetMapping("/page")
    public Result<Page<Reservation>> page(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Reservation> page = new Page<>(pageNum, pageSize);
        return Result.success(reservationService.page(page));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody Reservation reservation) {
        return Result.success(reservationService.saveOrUpdate(reservation));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(reservationService.removeById(id));
    }
}
