package com.gym.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gym.entity.Menu;
import java.util.List;

public interface MenuService extends IService<Menu> {
    // 获取当前用户的所有可用菜单
    List<Menu> getMenusByRole(String role);
}
