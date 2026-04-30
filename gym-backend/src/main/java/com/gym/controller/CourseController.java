package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.common.Result;
import com.gym.entity.Course;
import com.gym.service.MenuPermissionService;
import com.gym.service.CourseService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/course")
@CrossOrigin
@PreAuthorize("isAuthenticated()")
public class CourseController {
    private static final String COURSE_MENU_PATH = "/course";

    private final CourseService courseService;
    private final MenuPermissionService menuPermissionService;

    public CourseController(CourseService courseService, MenuPermissionService menuPermissionService) {
        this.courseService = courseService;
        this.menuPermissionService = menuPermissionService;
    }

    @GetMapping("/page")
    public Result<Page<Course>> page(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "") String name) {
        if (!menuPermissionService.hasMenuAction(COURSE_MENU_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看课程");
        }
        Page<Course> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        if (!"".equals(name)) {
            wrapper.like(Course::getName, name);
        }
        return Result.success(courseService.page(page, wrapper));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody Course course) {
        String action = course.getId() == null ? MenuPermissionService.ACTION_CREATE : MenuPermissionService.ACTION_UPDATE;
        if (!menuPermissionService.hasMenuAction(COURSE_MENU_PATH, action)) {
            return denied("维护课程");
        }
        return Result.success(courseService.saveOrUpdate(course));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        if (!menuPermissionService.hasMenuAction(COURSE_MENU_PATH, MenuPermissionService.ACTION_DELETE)) {
            return denied("删除课程");
        }
        return Result.success(courseService.removeById(id));
    }

    private <T> Result<T> denied(String actionLabel) {
        return Result.error("当前角色无权限执行操作: " + actionLabel);
    }
}
