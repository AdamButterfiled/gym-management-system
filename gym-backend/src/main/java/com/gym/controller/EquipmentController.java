package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.common.Result;
import com.gym.entity.Equipment;
import com.gym.service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/equipment")
@CrossOrigin
public class EquipmentController {

    @Autowired
    private EquipmentService equipmentService;

    @GetMapping("/page")
    public Result<Page<Equipment>> page(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "") String name) {
        Page<Equipment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<>();
        if (!"".equals(name)) {
            wrapper.like(Equipment::getName, name);
        }
        return Result.success(equipmentService.page(page, wrapper));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody Equipment equipment) {
        return Result.success(equipmentService.saveOrUpdate(equipment));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(equipmentService.removeById(id));
    }
}
