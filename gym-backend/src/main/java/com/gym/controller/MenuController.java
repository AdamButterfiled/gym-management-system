package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.common.Result;
import com.gym.entity.Menu;
import com.gym.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 菜单管理控制器
 * 1. 提供给前端【动态路由】加载使用 (根据用户角色返回菜单树)
 * 2. 提供给【管理员】进行菜单的增删改查
 */
@RestController
@RequestMapping("/menu")
@CrossOrigin
public class MenuController {

    @Autowired
    private MenuService menuService;

    /**
     * 获取当前用户的动态菜单 (树形结构需前端组装或后端组装，这里返回列表让前端处理更灵活)
     */
    @GetMapping("/user-menus")
    public Result<List<Menu>> getUserMenus(@RequestParam String role) {
        return Result.success(menuService.getMenusByRole(role));
    }

    /**
     * 菜单管理: 分页查询
     */
    @GetMapping("/page")
    public Result<Page<Menu>> page(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "") String title) {
        Page<Menu> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        if (!"".equals(title)) {
            wrapper.like(Menu::getTitle, title);
        }
        wrapper.orderByAsc(Menu::getSort);
        return Result.success(menuService.page(page, wrapper));
    }

    /**
     * 菜单管理: 新增/修改
     */
    @PostMapping
    public Result<Boolean> save(@RequestBody Menu menu) {
        return Result.success(menuService.saveOrUpdate(menu));
    }

    /**
     * 菜单管理: 删除
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(menuService.removeById(id));
    }

    /**
     * 菜单管理: 获取列表 (非分页，用于构建父级菜单选择树)
     */
    @GetMapping("/list")
    public Result<List<Menu>> list() {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Menu::getSort);
        return Result.success(menuService.list(wrapper));
    }
}
