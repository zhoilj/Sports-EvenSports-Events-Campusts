package com.sports.security.config;

import com.sports.security.filter.JwtAuthenticationFilter;
import com.sports.security.handler.JwtAccessDeniedHandler;
import com.sports.security.handler.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置
 *
 * @author sports
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    /**
     * 白名单路径
     */
    private static final String[] WHITE_LIST = {
            // 认证相关
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/captcha",
            // 公开接口
            "/api/public/**",
            // Swagger 文档
            "/doc.html",
            "/webjars/**",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            // 静态资源
            "/favicon.ico",
            "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（使用 JWT 不需要）
                .csrf(AbstractHttpConfigurer::disable)
                // 禁用 Session（使用 JWT 无状态）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置异常处理
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                // 配置请求授权
                .authorizeHttpRequests(auth -> auth
                        // 白名单放行
                        .requestMatchers(WHITE_LIST).permitAll()
                        // OPTIONS 请求放行
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 管理员接口
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 教师接口
                        .requestMatchers("/api/teacher/**").hasAnyRole("ADMIN", "TEACHER")
                        // 其他请求需要认证
                        .anyRequest().authenticated())
                // 添加 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}