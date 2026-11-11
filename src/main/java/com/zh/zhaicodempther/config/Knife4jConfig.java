package com.zh.zhaicodempther.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 文档配置
 * 文档元信息由 zh.api-doc.* 配置项注入，可在 application.yml 中按环境调整
 * 文档地址：http://localhost:8123/api/doc.html
 */
@Configuration
public class Knife4jConfig {

    private final String title;
    private final String description;
    private final String version;
    private final String contactName;

    public Knife4jConfig(@Value("${zh.api-doc.title:zh-ai-code-mpther 接口文档}") String title,
                         @Value("${zh.api-doc.description:基于 Spring Boot + Knife4j 的在线接口文档}") String description,
                         @Value("${zh.api-doc.version:v1.0.0}") String version,
                         @Value("${zh.api-doc.contact-name:zh}") String contactName) {
        this.title = title;
        this.description = description;
        this.version = version;
        this.contactName = contactName;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(new Info()
                .title(title)
                .description(description)
                .version(version)
                .contact(new Contact().name(contactName)));
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("全部接口")
                .pathsToMatch("/**")
                .build();
    }
}
