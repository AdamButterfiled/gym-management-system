package com.gym.controller;

import cn.hutool.core.lang.UUID;
import com.gym.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

// 1. 我们为扫码登录创建了一个简单的控制器。
// 2. 在实际项目中，可以使用 Redis 存储 UUID 和登录状态。
// 3. 这里我们使用一个简单的 Map 或者直接通过 UUID 模拟状态。

@RestController
public class QrCodeController {

    // 模拟的 UUID 存储
    private static final Map<String, String> QR_STATUS_MAP = new HashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    private com.gym.service.UserService userService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.gym.utils.JwtUtils jwtUtils;

    /**
     * 第一步：前端请求通过 /api/qrcode 获取一个 UUID。
     * 响应：{"uuid": "xxx-xxx-xxx"}
     */
    @GetMapping("/api/qrcode")
    public Result<Map<String, String>> getQrCode() {
        // 生成一个新的 UUID
        String uuid = UUID.randomUUID().toString();
        // 初始化状态为 "WAITING"
        QR_STATUS_MAP.put(uuid, "WAITING");

        Map<String, String> map = new HashMap<>();
        map.put("uuid", uuid);

        // 打印调试信息：告诉开发者这个 UUID 有效
        // System.out.println("[QrCodeController] Generated UUID: " + uuid);
        return Result.success(map);
    }

    /**
     * 第二步：前端轮询 /api/check-status
     * 响应：如果状态变为 "SUCCESS"，则返回成功。
     * 这里我们做一个简单逻辑：如果 UUID 存在，且这里我们假设用户已经在手机上确认了（模拟手机确认），
     * 为了调试方便，我们这里假设一定时间后或者立即通过验证，方便没有手机端配合时展示效果。
     * 或者配合前端的 "debug link" 触发。
     */
    @GetMapping("/api/check-status")
    public Result<Map<String, Object>> checkStatus(@RequestParam String uuid) {
        String status = QR_STATUS_MAP.get(uuid);
        if (status == null) {
            return Result.error("Invalid UUID or Expired");
        }

        // 在真实场景中：手机端扫描并确认后，会把后端 QR_STATUS_MAP 改为 "SUCCESS" 并存入 login user info。
        // 为了演示：我们这里假设只要查询就算成功，或者你需要手动触发一个 endpoint /api/confirm?uuid=xxx
        // 让我们创建一个确认接口。

        if ("SUCCESS".equals(status)) {
            // 返回真实的 Token
            // 假设我们默认登录的是 admin 用户（演示用）
            com.gym.entity.User user = userService
                    .getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.gym.entity.User>()
                            .eq(com.gym.entity.User::getUsername, "admin"));

            if (user != null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("uid", user.getId());
                payload.put("username", user.getUsername());
                payload.put("role", user.getRole());

                String token = jwtUtils.generateToken(user.getUsername(), payload);
                user.setToken(token);

                Map<String, Object> data = new HashMap<>();
                data.put("status", "SUCCESS");
                data.put("user", user);

                // 登录成功后移除 UUID 防止重复使用
                QR_STATUS_MAP.remove(uuid);

                return Result.success(data);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("status", "WAITING");
        return Result.success(data);
    }

    /**
     * 手机端确认接口（模拟）
     */
    @GetMapping("/api/confirm")
    public Result<String> confirm(@RequestParam String uuid) {
        if (QR_STATUS_MAP.containsKey(uuid)) {
            QR_STATUS_MAP.put(uuid, "SUCCESS");
            return Result.success("Confirmed!");
        }
        return Result.error("Invalid UUID");
    }
}
