package com.sports.test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码验证测试
 * 直接运行此类来测试和生成密码
 */
public class PasswordTest {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String rawPassword = "123456";

        System.out.println("=".repeat(60));
        System.out.println("密码测试工具");
        System.out.println("=".repeat(60));

        // 生成新的哈希
        String newHash = encoder.encode(rawPassword);
        System.out.println("\n【新生成的哈希】");
        System.out.println("密码: " + rawPassword);
        System.out.println("哈希: " + newHash);

        // 验证新哈希
        boolean newMatch = encoder.matches(rawPassword, newHash);
        System.out.println("验证: " + (newMatch ? "✓ 成功" : "✗ 失败"));

        // 测试各种已知哈希
        System.out.println("\n【测试已有哈希】");
        String[] testHashes = {
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy",
            "$2a$10$EqKcp1WFKVQISheBxkVJceUVqTgdSPnPcox/pG0GNmvCY/S6FL0Ey",
            "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH"
        };

        for (String hash : testHashes) {
            boolean match = encoder.matches(rawPassword, hash);
            System.out.println(hash.substring(0, 40) + "...");
            System.out.println("  -> " + (match ? "✓ 匹配 123456" : "✗ 不匹配"));
        }

        // 输出 SQL
        System.out.println("\n" + "=".repeat(60));
        System.out.println("【请在 Navicat 中执行以下 SQL】");
        System.out.println("=".repeat(60));
        System.out.println("\nUPDATE sports_events.sys_user");
        System.out.println("SET password = '" + newHash + "'");
        System.out.println("WHERE is_deleted = 0;");
        System.out.println();
    }
}
