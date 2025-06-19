package com.example.managedredis.model;

import io.fabric8.kubernetes.api.model.Condition;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ManagedRedisStatus {
    private String phase;
    private String primaryEndpoint;
    private String readerEndpoint;
    private List<Condition> conditions = new ArrayList<>();
    private List<RedisNode> nodes = new ArrayList<>();

    @Data
    public static class RedisNode {
        private String name;
        private String role;
        private String status;
        private String endpoint;
    }
} 