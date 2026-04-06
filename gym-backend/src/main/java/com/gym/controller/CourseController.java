package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.common.Result;
import com.gym.entity.Course;
import com.gym.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/course")
@CrossOrigin
public class CourseController {

    @Autowired
    private CourseService courseService;

    @GetMapping("/page")
    public Result<Page<Course>> page(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "") String name) {
        Page<Course> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        if (!"".equals(name)) {
            wrapper.like(Course::getName, name);
        }
        return Result.success(courseService.page(page, wrapper));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody Course course) {
        return Result.success(courseService.saveOrUpdate(course));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(courseService.removeById(id));
    }
}
