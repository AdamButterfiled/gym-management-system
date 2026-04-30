package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.common.Result;
import com.gym.entity.Repair;
import com.gym.service.MenuPermissionService;
import com.gym.service.RepairService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/repair")
@CrossOrigin
@PreAuthorize("isAuthenticated()")
public class RepairController {
    private static final String REPAIR_MENU_PATH = "/repair";

    private static final Set<String> ALLOWED_STATUS = Set.of("PENDING", "PROCESSING", "FIXED");

    private final RepairService repairService;
    private final MenuPermissionService menuPermissionService;

    public RepairController(RepairService repairService, MenuPermissionService menuPermissionService) {
        this.repairService = repairService;
        this.menuPermissionService = menuPermissionService;
    }

    @GetMapping("/page")
    public Result<Page<Repair>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                     @RequestParam(defaultValue = "") String keyword,
                                     @RequestParam(defaultValue = "") String status) {
        if (!menuPermissionService.hasMenuAction(REPAIR_MENU_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看报修工单");
        }
        Page<Repair> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Repair> wrapper = new LambdaQueryWrapper<>();
        if (!keyword.isBlank()) {
            wrapper.like(Repair::getDescription, keyword.trim());
        }
        if (!status.isBlank()) {
            wrapper.eq(Repair::getStatus, status.trim());
        }
        wrapper.orderByDesc(Repair::getCreatedAt);
        return Result.success(repairService.page(page, wrapper));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody Repair repair) {
        String action = repair.getId() == null ? MenuPermissionService.ACTION_CREATE : MenuPermissionService.ACTION_UPDATE;
        if (!menuPermissionService.hasMenuAction(REPAIR_MENU_PATH, action)) {
            return denied("维护报修工单");
        }
        normalizeRepair(repair);
        return Result.success(repairService.saveOrUpdate(repair));
    }

    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        if (!menuPermissionService.hasMenuAction(REPAIR_MENU_PATH, MenuPermissionService.ACTION_UPDATE)) {
            return denied("更新报修状态");
        }
        String status = payload == null ? null : payload.get("status");
        if (status == null || !ALLOWED_STATUS.contains(status)) {
            return Result.error("无效的工单状态");
        }

        Repair repair = repairService.getById(id);
        if (repair == null) {
            return Result.error("工单不存在");
        }

        repair.setStatus(status);
        return Result.success(repairService.updateById(repair));
    }

    private void normalizeRepair(Repair repair) {
        if (repair.getCreatedAt() == null) {
            repair.setCreatedAt(LocalDateTime.now());
        }
        if (repair.getStatus() == null || repair.getStatus().isBlank()) {
            repair.setStatus("PENDING");
        }
    }

    private <T> Result<T> denied(String actionLabel) {
        return Result.error("当前角色无权限执行操作: " + actionLabel);
    }
}
