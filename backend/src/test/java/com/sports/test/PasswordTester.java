package com.sports.test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码验证测试工具
 * 用于测试密码和哈希是否匹配
 */
public class PasswordTester {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println("=".repeat(80));
        System.out.println("密码验证测试工具");
        System.out.println("=".repeat(80));
        System.out.println();

        // 数据库中当前的密码哈希（从日志中复制）
        String currentHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH";

        // 测试多个密码
        String[] testPasswords = {"123456", "admin123", "admin", "password", "root"};

        System.out.println("当前数据库密码哈希:");
        System.out.println(currentHash);
        System.out.println();
        System.out.println("测试结果:");
        System.out.println("-".repeat(80));

        boolean foundMatch = false;
        for (String password : testPasswords) {
            boolean matches = encoder.matches(password, currentHash);
            String result = matches ? "✓ 匹配" : "✗ 不匹配";
            System.out.printf("密码: %-15s %s%n", password, result);

            if (matches && !foundMatch) {
                foundMatch = true;
                System.out.println("  --> 找到匹配的密码！请使用此密码登录");
            }
        }

        System.out.println("-".repeat(80));
        System.out.println();

        if (!foundMatch) {
            System.out.println("警告: 未找到匹配的密码！");
            System.out.println();
            System.out.println("建议操作:");
            System.out.println("1. 生成新的密码哈希:");
            System.out.println("   运行 PasswordGenerator.java");
            System.out.println();
            System.out.println("2. 更新数据库密码:");
            System.out.println("   执行 fix_admin_password_multiple.sql");
            System.out.println();
            System.out.println("3. 重启应用测试");
        } else {
            System.out.println("✓ 密码验证成功！");
        }

        System.out.println();
        System.out.println("如需生成新密码哈希，请运行 PasswordGenerator.java");
    }
}
