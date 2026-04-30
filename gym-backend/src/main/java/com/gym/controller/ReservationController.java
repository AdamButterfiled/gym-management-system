package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.common.Result;
import com.gym.entity.Reservation;
import com.gym.service.MenuPermissionService;
import com.gym.service.ReservationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservation")
@CrossOrigin
@PreAuthorize("isAuthenticated()")
public class ReservationController {
    private static final String RESERVATION_MENU_PATH = "/reservation";

    private final ReservationService reservationService;
    private final MenuPermissionService menuPermissionService;

    public ReservationController(ReservationService reservationService, MenuPermissionService menuPermissionService) {
        this.reservationService = reservationService;
        this.menuPermissionService = menuPermissionService;
    }

    @GetMapping("/page")
    public Result<Page<Reservation>> page(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        if (!menuPermissionService.hasMenuAction(RESERVATION_MENU_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看预约");
        }
        Page<Reservation> page = new Page<>(pageNum, pageSize);
        return Result.success(reservationService.page(page));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody Reservation reservation) {
        String action = reservation.getId() == null ? MenuPermissionService.ACTION_CREATE : MenuPermissionService.ACTION_UPDATE;
        if (!menuPermissionService.hasMenuAction(RESERVATION_MENU_PATH, action)) {
            return denied("维护预约");
        }
        return Result.success(reservationService.saveOrUpdate(reservation));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        if (!menuPermissionService.hasMenuAction(RESERVATION_MENU_PATH, MenuPermissionService.ACTION_DELETE)) {
            return denied("删除预约");
        }
        return Result.success(reservationService.removeById(id));
    }

    private <T> Result<T> denied(String actionLabel) {
        return Result.error("当前角色无权限执行操作: " + actionLabel);
    }
}
