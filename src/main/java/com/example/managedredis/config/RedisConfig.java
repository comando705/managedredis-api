package com.example.managedredis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "redis")
public class RedisConfig {
    public static final String REDIS_IMAGE = "redis:%s";
    public static final String APP_LABEL = "app";
    public static final String ROLE_LABEL = "role";
    public static final String MANAGED_BY_LABEL = "managed-by";
    public static final String REDIS_PORT = "6379";
    
    public static final String PRIMARY_ROLE = "Primary";
    public static final String REPLICA_ROLE = "Replica";
    public static final String MANAGED_BY = "managedredis-operator";

    // Health check configuration
    private HealthCheck healthCheck = new HealthCheck();

    public static class HealthCheck {
        private long initialDelay = 30; // seconds
        private long period = 30; // seconds
        private int threadPoolSize = 1;

        public long getInitialDelay() {
            return initialDelay;
        }

        public void setInitialDelay(long initialDelay) {
            this.initialDelay = initialDelay;
        }

        public long getPeriod() {
            return period;
        }

        public void setPeriod(long period) {
            this.period = period;
        }

        public int getThreadPoolSize() {
            return threadPoolSize;
        }

        public void setThreadPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
        }
    }

    public HealthCheck getHealthCheck() {
        return healthCheck;
    }

    public void setHealthCheck(HealthCheck healthCheck) {
        this.healthCheck = healthCheck;
    }
} 