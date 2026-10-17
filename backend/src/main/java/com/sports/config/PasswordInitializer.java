package com.sports.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

/**
 * 本地一次性密码维护工具。默认不执行，只有显式启用 local-password-reset profile 才会运行。
 * 生产环境不得启用此 profile。
 */
@Slf4j
@Component
@Profile("local-password-reset")
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
            log.info("=====================================");

        } catch (Exception e) {
            log.error("密码重置失败: {}", e.getMessage());
        }
    }
}
