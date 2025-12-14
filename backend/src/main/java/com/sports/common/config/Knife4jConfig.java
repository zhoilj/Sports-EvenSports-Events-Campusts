package com.sports.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j (Swagger) 配置
 *
 * @author sports
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("校园体育赛事管理系统 API")
                        .description("基于 SpringBoot + Vue 的校园体育赛事管理系统接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Sports Team")
                                .email("sports@example.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .schemaRequirement("Bearer", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization")
                        .description("JWT Token"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"));
    }
}