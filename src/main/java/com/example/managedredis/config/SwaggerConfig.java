package com.example.managedredis.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI managedRedisOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Managed Redis API")
                        .description("Redis 클러스터를 관리하기 위한 REST API와 Kubernetes Controller")
                        .version("v1")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }
} 