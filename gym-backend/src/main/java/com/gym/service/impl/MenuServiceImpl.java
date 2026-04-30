package com.gym.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.entity.Menu;
import com.gym.entity.User;
import com.gym.mapper.MenuMapper;
import com.gym.mapper.UserMapper;
import com.gym.service.MenuPermissionService;
import com.gym.service.MenuService;
import com.gym.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {
    private final MenuPermissionService menuPermissionService;
    private final UserMapper userMapper;

    public MenuServiceImpl(MenuPermissionService menuPermissionService, UserMapper userMapper) {
        this.menuPermissionService = menuPermissionService;
        this.userMapper = userMapper;
    }

    @Override
    public List<Menu> getMenusByRole(String role) {
        List<Menu> allMenus = listAllMenus();
        Map<Long, Menu> menuMap = allMenus.stream()
                .filter(menu -> menu.getId() != null)
                .collect(Collectors.toMap(Menu::getId, Function.identity(), (left, right) -> left));

        return allMenus.stream()
                .filter(menu -> hasRoleAccess(menu, role))
                .filter(menu -> isMenuEnabled(menu, menuMap))
                .peek(menu -> menu.setGrantedActions(menuPermissionService.resolveGrantedActions(menu, role)))
                .collect(Collectors.toList());
    }

    @Override
    public List<Menu> getMenusForCurrentUser() {
        String currentUsername = SecurityUtils.getCurrentUsername();
        if (!StringUtils.hasText(currentUsername)) {
            return List.of();
        }

        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getUsername, currentUsername).last("LIMIT 1");
        User currentUser = userMapper.selectOne(userWrapper);
        if (currentUser == null) {
            return List.of();
        }

        List<Menu> allMenus = listAllMenus();
        Map<Long, Menu> menuMap = allMenus.stream()
                .filter(menu -> menu.getId() != null)
                .collect(Collectors.toMap(Menu::getId, Function.identity(), (left, right) -> left));
        Map<Long, List<String>> actionsByMenuId = new java.util.LinkedHashMap<>();
        Set<Long> visibleMenuIds = new LinkedHashSet<>();

        for (Menu menu : allMenus) {
            if (menu.getId() == null || !isMenuEnabled(menu, menuMap)) {
                continue;
            }

            List<String> grantedActions = menuPermissionService.resolveEffectiveActions(menu, currentUser);
            if (!grantedActions.contains(MenuPermissionService.ACTION_VIEW)) {
                continue;
            }

            visibleMenuIds.add(menu.getId());
            actionsByMenuId.put(menu.getId(), new ArrayList<>(grantedActions));
            includeAncestors(menu, menuMap, visibleMenuIds, actionsByMenuId);
        }

        return allMenus.stream()
                .filter(menu -> menu.getId() != null && visibleMenuIds.contains(menu.getId()))
                .peek(menu -> menu.setGrantedActions(new ArrayList<>(
                        actionsByMenuId.getOrDefault(menu.getId(), List.of(MenuPermissionService.ACTION_VIEW))
                )))
                .collect(Collectors.toList());
    }

    private List<Menu> listAllMenus() {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Menu::getSort);
        return this.list(wrapper);
    }

    private void includeAncestors(
            Menu menu,
            Map<Long, Menu> menuMap,
            Set<Long> visibleMenuIds,
            Map<Long, List<String>> actionsByMenuId
    ) {
        Long parentId = menu.getParentId();
        while (parentId != null && parentId != 0) {
            Menu parent = menuMap.get(parentId);
            if (parent == null || Boolean.TRUE.equals(parent.getHidden()) || parent.getId() == null) {
                break;
            }

            visibleMenuIds.add(parent.getId());
            actionsByMenuId.putIfAbsent(parent.getId(), new ArrayList<>(List.of(MenuPermissionService.ACTION_VIEW)));
            parentId = parent.getParentId();
        }
    }

    private boolean hasRoleAccess(Menu menu, String role) {
        if (menu.getRoles() == null || role == null) {
            return false;
        }
        return java.util.Arrays.stream(menu.getRoles().split(","))
                .map(String::trim)
                .anyMatch(role::equals);
    }

    private boolean isMenuEnabled(Menu menu, Map<Long, Menu> menuMap) {
        Menu current = menu;

        while (current != null) {
            if (Boolean.TRUE.equals(current.getHidden())) {
                return false;
            }

            Long parentId = current.getParentId();
            if (parentId == null || parentId == 0) {
                return true;
            }

            current = menuMap.get(parentId);
        }

        return true;
    }
}
