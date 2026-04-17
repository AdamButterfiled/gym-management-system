package com.gym.security;

import com.gym.config.SpaRouteSupport;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // 引入 BCrypt 加密工具
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // 引入原生过滤器
import org.springframework.stereotype.Component;

/**
 * 核心安全配置类：SecurityConfig
 * 负责定义哪些请求可以匿名访问，哪些必须认证，以及如何处理密码和 JWT。
 */
@Component
@EnableWebSecurity
@EnableMethodSecurity // 开启方法级别的安全控制（如 @PreAuthorize）
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 构造注入：将我们自定义的 JWT 过滤器注入进来
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 定义 Spring Security 的过滤链 (SecurityFilterChain)。
     * 这里的每一行配置都决定了请求如何被处理。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 关闭 CSRF 保护：因为我们使用 JWT 无状态认证，不需要防止 CSRF 攻击。
                .csrf(csrf -> csrf.disable())

                // 2. 授权管理：决定哪些 URL 需要权限，哪些可以直接访问。
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()

                        // 下面这些路径允许所有人访问 (permitAll)
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/user/login",
                                "/user/register",
                                "/user/export",
                                "/user/import",
                                "/api/qrcode",
                                "/api/check-status",
                                "/api/confirm",
                                "/error", // 允许访问默认错误页面
                                "/favicon.ico", // 允许访问图标
                                "/v3/api-docs/**", // 如果有 Swagger 文档
                                "/swagger-ui/**")
                        .permitAll()

                        // 允许浏览器直接访问前端页面路由，交给 Vue Router 接管。
                        .requestMatchers(SpaRouteSupport.frontendRouteMatcher()).permitAll()

                        // 允许所有的 OPTIONS 请求 (解决跨域预检问题)
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                        // 其他任何请求都需要经过身份认证 (authenticated)
                        .anyRequest().authenticated())

                // 3. 会话管理：设置为无状态 (STATELESS)。
                // 这意味着服务器不保存 Session，每个请求都必须带 Token。
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. 添加过滤器：把我们的 JwtAuthenticationFilter 加在 Spring Security 标准的
                // UsernamePasswordAuthenticationFilter 之前。
                // 这样每次请求都会先检查有没有 Token。
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // 配置密码加密器：使用 BCrypt 对密码进行 Hash 加密
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
