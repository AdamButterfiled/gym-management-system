package com.gym.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.entity.Menu;
import com.gym.mapper.MenuMapper;
import com.gym.service.MenuService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    @Override
    public List<Menu> getMenusByRole(String role) {
        // 这里做一个简单的模拟过滤
        // 实际场景最好有角色-菜单关联表，或者 role 字段存了 "ADMIN,STAFF"
        // 假设 menu.roles 存储 "ADMIN,STAFF"
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Menu::getSort);

        List<Menu> allMenus = this.list(wrapper);
        Map<Long, Menu> menuMap = allMenus.stream()
                .filter(menu -> menu.getId() != null)
                .collect(Collectors.toMap(Menu::getId, Function.identity(), (left, right) -> left));

        return allMenus.stream()
                .filter(menu -> hasRoleAccess(menu, role))
                .filter(menu -> isMenuEnabled(menu, menuMap))
                .collect(Collectors.toList());
    }

    private boolean hasRoleAccess(Menu menu, String role) {
        if ("ADMIN".equals(role)) {
            return true;
        }

        return menu.getRoles() != null && menu.getRoles().contains(role);
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
