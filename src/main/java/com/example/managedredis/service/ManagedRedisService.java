package com.example.managedredis.service;

import com.example.managedredis.model.ManagedRedis;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ManagedRedisService {

    private final KubernetesClient kubernetesClient;
    private static final String DEFAULT_NAMESPACE = "default";

    public ManagedRedisService(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    public ManagedRedis create(String namespace, ManagedRedis managedRedis) {
        String ns = Optional.ofNullable(namespace).orElse(DEFAULT_NAMESPACE);
        return kubernetesClient.resources(ManagedRedis.class)
                .inNamespace(ns)
                .create(managedRedis);
    }

    public List<ManagedRedis> list(String namespace) {
        String ns = Optional.ofNullable(namespace).orElse(DEFAULT_NAMESPACE);
        return kubernetesClient.resources(ManagedRedis.class)
                .inNamespace(ns)
                .list()
                .getItems();
    }

    public ManagedRedis get(String namespace, String name) {
        String ns = Optional.ofNullable(namespace).orElse(DEFAULT_NAMESPACE);
        return kubernetesClient.resources(ManagedRedis.class)
                .inNamespace(ns)
                .withName(name)
                .get();
    }

    public void delete(String namespace, String name) {
        String ns = Optional.ofNullable(namespace).orElse(DEFAULT_NAMESPACE);
        kubernetesClient.resources(ManagedRedis.class)
                .inNamespace(ns)
                .withName(name)
                .delete();
    }
} 