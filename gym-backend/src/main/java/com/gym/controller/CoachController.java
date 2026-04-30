package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.common.Result;
import com.gym.entity.Coach;
import com.gym.service.CoachService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 教练管理控制器
 * 提供前端调用的 RESTful 接口
 */
@RestController // 标识为返回 JSON 的控制器
@RequestMapping("/coach") // 基础路由路径
@CrossOrigin // 允许跨域访问
@PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
public class CoachController {

    @Autowired
    private CoachService coachService;

    /**
     * 分页查询获取教练列表
     * 
     * @param pageNum  当前页码，默认 1
     * @param pageSize 每页条数，默认 10
     * @param name     教练姓名（模糊查询），可选
     * @return 分页结果数据
     */
    @GetMapping("/page")
    public Result<Page<Coach>> page(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "") String name) {
        // 1. 创建分页对象
        Page<Coach> page = new Page<>(pageNum, pageSize);

        // 2. 构建查询条件
        LambdaQueryWrapper<Coach> wrapper = new LambdaQueryWrapper<>();

        // 如果传入了姓名，则添加模糊查询条件
        if (StringUtils.hasText(name)) {
            wrapper.like(Coach::getName, name);
        }

        // 按 ID 倒序排列，新添加的在前面
        wrapper.orderByDesc(Coach::getId);

        // 3. 执行查询并返回结果
        return Result.success(coachService.page(page, wrapper));
    }

    /**
     * 新增或更新教练信息
     * 
     * @param coach 前端传递的 JSON 对象映射为 Coach 实体
     * @return 操作结果
     */
    @PostMapping
    public Result<Boolean> save(@RequestBody Coach coach) {
        // saveOrUpdate 是 MyBatis-Plus 提供的方法
        // 如果 ID 存在则更新，不存在则插入
        return Result.success(coachService.saveOrUpdate(coach));
    }

    /**
     * 根据 ID 删除教练
     * 
     * @param id URL 路径上的 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(coachService.removeById(id));
    }

    /**
     * 根据 ID 获取单个教练详情
     * 
     * @param id URL 路径上的 ID
     * @return 教练实体
     */
    @GetMapping("/{id}")
    public Result<Coach> getById(@PathVariable Long id) {
        return Result.success(coachService.getById(id));
    }
}
