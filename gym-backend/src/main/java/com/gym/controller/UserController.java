package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.common.Result;
import com.gym.entity.User;
import com.gym.service.MenuPermissionService;
import com.gym.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@CrossOrigin // Allow cross-origin for frontend
public class UserController {
    private static final String USER_MENU_PATH = "/user";
    private static final String PERMISSION_MANAGEMENT_PATH = "/sys/permissions";

    private final UserService userService;
    private final MenuPermissionService menuPermissionService;

    public UserController(UserService userService, MenuPermissionService menuPermissionService) {
        this.userService = userService;
        this.menuPermissionService = menuPermissionService;
    }

    @PostMapping("/login")
    public Result<User> login(@RequestBody User user) {
        User loggedInUser = userService.login(user.getUsername(), user.getPassword());
        if (loggedInUser != null) {
            return Result.success(loggedInUser);
        }
        return Result.error("Invalid username or password");
    }

    @PostMapping("/register")
    public Result<Boolean> register(@RequestBody User user) {
        return Result.success(userService.register(user));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/page")
    public Result<Page<User>> page(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "") String username) {
        if (!menuPermissionService.hasMenuAction(USER_MENU_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看用户信息");
        }
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (!"".equals(username)) {
            wrapper.like(User::getUsername, username);
        }
        return Result.success(userService.page(page, wrapper));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/permission-subjects")
    public Result<List<User>> permissionSubjects(@RequestParam(defaultValue = "") String keyword) {
        if (!menuPermissionService.hasMenuAction(PERMISSION_MANAGEMENT_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看权限对象");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(
                User::getId,
                User::getUsername,
                User::getRealName,
                User::getNickname,
                User::getRole,
                User::getStatus,
                User::getPermissionConfig
        );
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getRealName, keyword)
                    .or()
                    .like(User::getNickname, keyword)
            );
        }
        wrapper.orderByAsc(User::getRole).orderByAsc(User::getId);
        return Result.success(userService.list(wrapper));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}/permission-config")
    public Result<Boolean> updatePermissionConfig(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> payload) {
        if (!menuPermissionService.hasMenuAction(PERMISSION_MANAGEMENT_PATH, MenuPermissionService.ACTION_UPDATE)) {
            return denied("维护账号权限");
        }

        Object rawPermissionConfig = payload == null ? null : payload.get("permissionConfig");
        User user = new User();
        user.setId(id);
        user.setPermissionConfig(menuPermissionService.sanitizeUserPermissionConfig(
                rawPermissionConfig == null ? null : String.valueOf(rawPermissionConfig)
        ));
        return Result.success(userService.updateById(user));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public Result<Boolean> save(@RequestBody User user) {
        String action = user.getId() == null ? MenuPermissionService.ACTION_CREATE : MenuPermissionService.ACTION_UPDATE;
        if (!menuPermissionService.hasMenuAction(USER_MENU_PATH, action)) {
            return denied("维护用户信息");
        }
        return Result.success(userService.saveOrUpdate(user));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/batch")
    public Result<Boolean> batchDelete(@RequestBody List<Long> ids) {
        if (!menuPermissionService.hasMenuAction(USER_MENU_PATH, MenuPermissionService.ACTION_DELETE)) {
            return denied("批量删除用户");
        }
        if (ids == null || ids.isEmpty()) {
            return Result.success(true);
        }
        return Result.success(userService.removeByIds(ids));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        if (!menuPermissionService.hasMenuAction(USER_MENU_PATH, MenuPermissionService.ACTION_DELETE)) {
            return denied("删除用户");
        }
        return Result.success(userService.removeById(id));
    }

    private <T> Result<T> denied(String actionLabel) {
        return Result.error("当前角色无权限执行操作: " + actionLabel);
    }
}
