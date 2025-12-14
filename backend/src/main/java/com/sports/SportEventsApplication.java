package com.sports;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 校园体育赛事管理系统启动类
 *
 * @author sports
 */
@SpringBootApplication
@MapperScan("com.sports.modules.*.mapper")
public class SportEventsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportEventsApplication.class, args);
        System.out.println("========================================");
        System.out.println("  校园体育赛事管理系统启动成功！");
        System.out.println("  接口文档: http://localhost:8080/api/doc.html");
        System.out.println("========================================");
    }
}