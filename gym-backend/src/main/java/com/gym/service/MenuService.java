package com.gym.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gym.entity.Menu;
import java.util.List;

public interface MenuService extends IService<Menu> {
    List<Menu> getMenusByRole(String role);

    List<Menu> getMenusForCurrentUser();
}
