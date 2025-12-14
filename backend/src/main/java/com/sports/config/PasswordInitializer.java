package com.sports.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据库初始化 - 重置所有用户密码为 123456
 * 启动时自动执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            // 生成 123456 的 BCrypt 哈希
            String newPassword = "123456";
            String encodedPassword = passwordEncoder.encode(newPassword);

            // 更新所有用户密码
            int updated = jdbcTemplate.update(
                "UPDATE sys_user SET password = ? WHERE is_deleted = 0",
                encodedPassword
            );

            log.info("=====================================");
            log.info("密码重置成功！");
            log.info("影响用户数: {}", updated);
            log.info("新密码: {}", newPassword);
            log.info("=====================================");
            log.info("登录信息:");
            log.info("  admin / 123456 (管理员)");
            log.info("  teacher01 / 123456 (教师)");
            log.info("  student01 / 123456 (学生)");
            log.info("=====================================");

        } catch (Exception e) {
            log.error("密码重置失败: {}", e.getMessage());
        }
    }
}
