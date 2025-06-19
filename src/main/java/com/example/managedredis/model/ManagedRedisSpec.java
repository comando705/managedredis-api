package com.example.managedredis.model;

import io.fabric8.kubernetes.api.model.ResourceRequirements;
import lombok.Data;

@Data
public class ManagedRedisSpec {
    private String version;
    private int replicas;
    private ResourceRequirements resources;
} 