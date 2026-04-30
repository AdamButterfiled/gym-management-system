package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.common.Result;
import com.gym.entity.Venue;
import com.gym.service.MenuPermissionService;
import com.gym.service.VenueService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/venue")
@CrossOrigin
@PreAuthorize("isAuthenticated()")
public class VenueController {
    private static final String VENUE_MENU_PATH = "/venue";

    private final VenueService venueService;
    private final MenuPermissionService menuPermissionService;

    public VenueController(VenueService venueService, MenuPermissionService menuPermissionService) {
        this.venueService = venueService;
        this.menuPermissionService = menuPermissionService;
    }

    @GetMapping("/page")
    public Result<Page<Venue>> page(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "") String name) {
        if (!menuPermissionService.hasMenuAction(VENUE_MENU_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看场馆");
        }
        Page<Venue> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Venue> wrapper = new LambdaQueryWrapper<>();
        if (!"".equals(name)) {
            wrapper.like(Venue::getName, name);
        }
        return Result.success(venueService.page(page, wrapper));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody Venue venue) {
        String action = venue.getId() == null ? MenuPermissionService.ACTION_CREATE : MenuPermissionService.ACTION_UPDATE;
        if (!menuPermissionService.hasMenuAction(VENUE_MENU_PATH, action)) {
            return denied("维护场馆");
        }
        return Result.success(venueService.saveOrUpdate(venue));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        if (!menuPermissionService.hasMenuAction(VENUE_MENU_PATH, MenuPermissionService.ACTION_DELETE)) {
            return denied("删除场馆");
        }
        return Result.success(venueService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<Venue> getById(@PathVariable Long id) {
        if (!menuPermissionService.hasMenuAction(VENUE_MENU_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看场馆详情");
        }
        return Result.success(venueService.getById(id));
    }

    private <T> Result<T> denied(String actionLabel) {
        return Result.error("当前角色无权限执行操作: " + actionLabel);
    }
}
