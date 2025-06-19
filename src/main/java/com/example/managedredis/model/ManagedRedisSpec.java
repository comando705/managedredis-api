package com.example.managedredis.model;

import io.fabric8.kubernetes.api.model.ResourceRequirements;
import lombok.Data;

@Data
public class ManagedRedisSpec {
    private String version;
    private int replicas;
    private Resources resources;

    @Data
    public static class Resources {
        private ResourceRequirements requests;
        private ResourceRequirements limits;
    }

    @Data
    public static class ResourceRequirements {
        private String cpu;
        private String memory;
    }
} 