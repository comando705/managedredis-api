package com.example.managedredis.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {
    public static final String REDIS_IMAGE = "redis:%s";
    public static final String APP_LABEL = "app";
    public static final String ROLE_LABEL = "role";
    public static final String MANAGED_BY_LABEL = "managed-by";
    public static final String REDIS_PORT = "6379";
    
    public static final String PRIMARY_ROLE = "primary";
    public static final String REPLICA_ROLE = "replica";
    public static final String MANAGED_BY = "managedredis-operator";
} 