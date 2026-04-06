package com.gym.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.entity.Menu;
import com.gym.mapper.MenuMapper;
import com.gym.service.MenuService;
import org.springframework.stereotype.Service;
import java.util.List;
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

        // 如果是超级管理员，返回所有
        if ("ADMIN".equals(role)) {
            return allMenus;
        }

        // 否则通过字符串包含来过滤 (简单实现)
        return allMenus.stream()
                .filter(menu -> menu.getRoles() != null && menu.getRoles().contains(role))
                .collect(Collectors.toList());
    }
}
