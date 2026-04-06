package com.gym.security;

import com.gym.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 过滤器：JwtAuthenticationFilter
 * 这个类会拦截每一个请求，检查请求头里有没有有效的 Tokens。
 * 如果有，它就把当前用户的信息告诉 Spring Security，让系统知道 "这个用户已经登录了"。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils; // 工具类：用来检查 Token 是否合法

    @Autowired
    private UserDetailsService userDetailsService; // 服务：用来从数据库加载用户信息

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 1. 获取 Authorization 请求头
        String authHeader = request.getHeader("Authorization");
        String jwt;
        String username;

        // 2. 检查头是否存在，并且是不是以 "Bearer " 开头
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7); // 去掉 "Bearer " 前缀，拿到真正的 Token 字符串

            // 3. 验证 Token 是否有效 (签名是否正确，是否过期)
            if (jwtUtils.validateToken(jwt)) {
                // 4. 从 Token 中提取用户名
                username = jwtUtils.getUsernameFromToken(jwt);

                // 5. 如果用户名存在，且当前系统上下文中还没有认证信息 (说明还没登录)
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 6. 从数据库加载这个用户的详细信息
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                    // 7. 创建一个认证对象 (Authentication Token)
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
                            null, userDetails.getAuthorities());

                    // 8. 设置请求的详细信息 (IP 等)
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 9. 关键步骤：把认证对象放到 SecurityContext 中。
                    // 这一步之后，Spring Security 就认为当前请求是 "已认证" 的了。
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }
        // 10. 继续执行下一个过滤器 (放行请求)
        filterChain.doFilter(request, response);
    }
}
