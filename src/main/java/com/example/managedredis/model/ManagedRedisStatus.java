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
    private List<Node> nodes;

    @Data
    public static class Node {
        private String name;
        private String role;
        private String status;
        private String endpoint;
    }
} 