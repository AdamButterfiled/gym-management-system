package com.gym.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gym.entity.User;
import com.gym.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 这是一个自动修复密码的工具组件。
 * <p>
 * 问题原因：
 * 引入 Spring Security 后，默认使用 BCrypt 算法验证密码。
 * 但数据库里现有的 update 之前的密码是明文（比如 "admin"），
 * 这会导致 "Encoded password does not look like BCrypt" 错误，无法登录。
 * <p>
 * 解决方法：
 * 这个类会在项目启动时自动运行。它会检查 "admin" 用户，
 * 如果发现密码不是加密格式，就自动把它加密并保存回数据库。
 */
@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(PasswordMigrationRunner.class);

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        checkAndUpdateUser("admin", "admin");
    }

    private void checkAndUpdateUser(String username, String defaultPassword) {
        // 1. 从数据库查找用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userService.getOne(wrapper);

        if (user != null) {
            String currentPwd = user.getPassword();

            // 2. 检查密码是否已经是加密格式
            // BCrypt 密文通常以 $2a$ 开头
            if (currentPwd != null && !currentPwd.startsWith("$2a$")) {
                logger.warn("检测到用户 [{}] 的密码是明文，正在自动进行加密修复...", username);

                // 3. 加密密码
                String encodedPwd = passwordEncoder.encode(defaultPassword);
                user.setPassword(encodedPwd);

                // 4. 更新数据库
                userService.updateById(user);

                logger.info("用户 [{}] 的密码修复成功！新密码哈希已保存。", username);
            } else {
                logger.info("用户 [{}] 的密码通过检查（已是加密格式）。", username);
            }
        } else {
            logger.info("未找到用户 [{}]，跳过修复。", username);
        }
    }
}
