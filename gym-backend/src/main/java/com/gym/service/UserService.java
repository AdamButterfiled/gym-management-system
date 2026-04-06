package com.gym.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gym.entity.User;

public interface UserService extends IService<User> {
    User login(String username, String password);

    boolean register(User user);
}
