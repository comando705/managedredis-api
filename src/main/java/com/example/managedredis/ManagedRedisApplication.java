package com.example.managedredis;

import io.javaoperatorsdk.operator.springboot.EnableJavaOperator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableJavaOperator
public class ManagedRedisApplication {
    public static void main(String[] args) {
        SpringApplication.run(ManagedRedisApplication.class, args);
    }
} 