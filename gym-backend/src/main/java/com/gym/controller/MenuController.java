package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.common.Result;
import com.gym.entity.Menu;
import com.gym.service.MenuPermissionService;
import com.gym.service.MenuService;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("isAuthenticated()")
public class MenuController {
    private static final String MENU_MANAGEMENT_PATH = "/sys/menu";
    private static final String FORM_CONFIG_PATH = "/sys/form-config";
    private static final String PERMISSION_MANAGEMENT_PATH = "/sys/permissions";

    private final MenuService menuService;
    private final MenuPermissionService menuPermissionService;

    public MenuController(MenuService menuService, MenuPermissionService menuPermissionService) {
        this.menuService = menuService;
        this.menuPermissionService = menuPermissionService;
    }

    /**
     * 获取当前用户的动态菜单 (树形结构需前端组装或后端组装，这里返回列表让前端处理更灵活)
     */
    @GetMapping("/user-menus")
    public Result<List<Menu>> getUserMenus() {
        return Result.success(menuService.getMenusForCurrentUser());
    }

    /**
     * 菜单管理: 分页查询
     */
    @GetMapping("/page")
    public Result<Page<Menu>> page(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "") String title) {
        if (!canViewMenuWorkspace()) {
            return denied("查看菜单权限");
        }
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
        String action = menu.getId() == null || menu.getId() == 0
                ? MenuPermissionService.ACTION_CREATE
                : MenuPermissionService.ACTION_UPDATE;
        if (!canManageMenuWorkspace(action)) {
            return denied("维护菜单权限");
        }
        return Result.success(menuService.saveOrUpdate(menuPermissionService.sanitizeMenu(menu)));
    }

    /**
     * 菜单管理: 删除
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        if (!canManageMenuWorkspace(MenuPermissionService.ACTION_DELETE)) {
            return denied("删除菜单");
        }
        return Result.success(menuService.removeById(id));
    }

    /**
     * 菜单管理: 获取列表 (非分页，用于构建父级菜单选择树)
     */
    @GetMapping("/list")
    public Result<List<Menu>> list() {
        if (!menuPermissionService.hasAnyMenuAction(
                MenuPermissionService.ACTION_VIEW,
                MENU_MANAGEMENT_PATH,
                FORM_CONFIG_PATH,
                PERMISSION_MANAGEMENT_PATH)) {
            return denied("查看菜单列表");
        }
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Menu::getSort);
        return Result.success(menuService.list(wrapper));
    }

    private boolean canViewMenuWorkspace() {
        return menuPermissionService.hasAnyMenuAction(
                MenuPermissionService.ACTION_VIEW,
                MENU_MANAGEMENT_PATH,
                PERMISSION_MANAGEMENT_PATH
        );
    }

    private boolean canManageMenuWorkspace(String action) {
        return menuPermissionService.hasAnyMenuAction(
                action,
                MENU_MANAGEMENT_PATH,
                PERMISSION_MANAGEMENT_PATH
        );
    }

    private <T> Result<T> denied(String actionLabel) {
        return Result.error("当前角色无权限执行操作: " + actionLabel);
    }
}
