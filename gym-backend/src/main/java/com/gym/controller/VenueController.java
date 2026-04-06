package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.common.Result;
import com.gym.entity.Venue;
import com.gym.service.VenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/venue")
@CrossOrigin
public class VenueController {

    @Autowired
    private VenueService venueService;

    @GetMapping("/page")
    public Result<Page<Venue>> page(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "") String name) {
        Page<Venue> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Venue> wrapper = new LambdaQueryWrapper<>();
        if (!"".equals(name)) {
            wrapper.like(Venue::getName, name);
        }
        return Result.success(venueService.page(page, wrapper));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody Venue venue) {
        return Result.success(venueService.saveOrUpdate(venue));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(venueService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<Venue> getById(@PathVariable Long id) {
        return Result.success(venueService.getById(id));
    }
}
