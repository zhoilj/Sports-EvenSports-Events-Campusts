package com.sports.test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码哈希生成工具
 * 用于生成 BCrypt 密码哈希
 */
public class PasswordGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 生成多个常用密码的哈希
        String[] passwords = {"123456", "admin123", "admin", "password"};

        System.out.println("=".repeat(80));
        System.out.println("BCrypt 密码哈希生成器");
        System.out.println("=".repeat(80));
        System.out.println();

        for (String password : passwords) {
            String hash = encoder.encode(password);
            System.out.println("原始密码: " + password);
            System.out.println("BCrypt哈希: " + hash);
            System.out.println();
            System.out.println("SQL 更新语句:");
            System.out.println("UPDATE sys_user SET password = '" + hash + "' WHERE username = 'admin';");
            System.out.println();
            System.out.println("-".repeat(80));
            System.out.println();
        }

        // 验证示例
        System.out.println("验证测试:");
        String testPassword = "123456";
        String testHash = encoder.encode(testPassword);
        boolean matches = encoder.matches(testPassword, testHash);
        System.out.println("密码: " + testPassword);
        System.out.println("哈希: " + testHash);
        System.out.println("验证结果: " + (matches ? "✓ 匹配" : "✗ 不匹配"));
    }
}
