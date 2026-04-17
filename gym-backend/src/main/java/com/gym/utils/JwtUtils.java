package com.gym.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtils {

    private static final String SECRET_KEY = "mySuperSecretKeyForGymManagementSystem123456";

    private static final long EXPIRATION_TIME = 86400000;

    private Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    /**
     * 生成 Token
     * 
     * @param username 用户名
     * @param claims   其他需要存入 Token 的信息 (如用户 ID, 角色等)
     * @return 加密后的 Token 字符串
     */
    public String generateToken(String username, Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims) // 放入自定义信息
                .setSubject(username) // 放入主题 (一般是用户名)
                .setIssuedAt(new Date()) // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 过期时间
                .signWith(key, SignatureAlgorithm.HS256) // 使用 HS256 算法和我们的密钥进行签名
                .compact(); // 压缩生成字符串
    }

    /**
     * 从 Token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    /**
     * 验证 Token 是否合法
     * 只要能成功解析 (没过期，签名正确)，就返回 true。
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // 如果签名不对，或者过期了，这里会抛出异常
            return false;
        }
    }
}
