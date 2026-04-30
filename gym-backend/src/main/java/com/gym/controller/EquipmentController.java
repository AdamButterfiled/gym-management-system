package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.common.Result;
import com.gym.entity.Equipment;
import com.gym.service.EquipmentService;
import com.gym.service.MenuPermissionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/equipment")
@CrossOrigin
@PreAuthorize("isAuthenticated()")
public class EquipmentController {
    private static final String EQUIPMENT_MENU_PATH = "/equipment";

    private final EquipmentService equipmentService;
    private final MenuPermissionService menuPermissionService;

    public EquipmentController(EquipmentService equipmentService, MenuPermissionService menuPermissionService) {
        this.equipmentService = equipmentService;
        this.menuPermissionService = menuPermissionService;
    }

    @GetMapping("/page")
    public Result<Page<Equipment>> page(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "") String name) {
        if (!menuPermissionService.hasMenuAction(EQUIPMENT_MENU_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看器材");
        }
        Page<Equipment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<>();
        if (!"".equals(name)) {
            wrapper.like(Equipment::getName, name);
        }
        return Result.success(equipmentService.page(page, wrapper));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody Equipment equipment) {
        String action = equipment.getId() == null ? MenuPermissionService.ACTION_CREATE : MenuPermissionService.ACTION_UPDATE;
        if (!menuPermissionService.hasMenuAction(EQUIPMENT_MENU_PATH, action)) {
            return denied("维护器材");
        }
        return Result.success(equipmentService.saveOrUpdate(equipment));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        if (!menuPermissionService.hasMenuAction(EQUIPMENT_MENU_PATH, MenuPermissionService.ACTION_DELETE)) {
            return denied("删除器材");
        }
        return Result.success(equipmentService.removeById(id));
    }

    private <T> Result<T> denied(String actionLabel) {
        return Result.error("当前角色无权限执行操作: " + actionLabel);
    }
}
