package com.gym.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.jwt.JWTUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.entity.User;
import com.gym.mapper.UserMapper;
import com.gym.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.gym.utils.JwtUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder; // 密码加密工具

    @Autowired
    private AuthenticationManager authenticationManager; // 认证管理器

    @Autowired
    private JwtUtils jwtUtils; // JWT 工具

    @Override
    public User login(String username, String password) {
        System.out.println("====== [DEBUG] 开始处理登录请求: " + username + " ======");

        // 1. 创建认证令牌
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username,
                password);
        try {
            // 2. 调用 Spring Security 进行认证
            // 这一步会自动调用 UserDetailsService.loadUserByUsername 来检查账号密码
            Authentication authenticate = authenticationManager.authenticate(authenticationToken);
            System.out.println("====== [DEBUG] 认证成功 (密码匹配) ======");

            // 3. 如果认证通过 (没抛异常)，从数据库获取完整用户信息
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, username);
            User user = baseMapper.selectOne(wrapper);

            // 4. 准备 Token 的载荷 (Payload)，放入我们需要的信息
            Map<String, Object> payload = new HashMap<>();
            payload.put("uid", user.getId());
            payload.put("username", user.getUsername());
            payload.put("role", user.getRole());

            // 5. 生成 Token
            String token = jwtUtils.generateToken(user.getUsername(), payload);
            user.setToken(token); // 把 Token 放进用户对象返回给前端

            System.out.println("====== [DEBUG] Token 生成完毕: " + token.substring(0, 10) + "... ======");
            return user;
        } catch (Exception e) {
            System.out.println("====== [DEBUG] 登录失败: " + e.getMessage() + " ======");
            // 认证失败
            return null;
        }
    }

    @Override
    public boolean register(User user) {
        // 1. 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        if (baseMapper.exists(wrapper)) {
            return false;
        }

        // 2. 如果没填密码，给一个默认值
        if (user.getPassword() == null) {
            user.setPassword("123456");
        }

        // 3. 关键：对密码进行加密存储！
        // 永远不要在数据库里存明文密码。
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 4. 设置默认角色
        if (user.getRole() == null) {
            user.setRole("MEMBER");
        }
        return save(user);
    }
}
