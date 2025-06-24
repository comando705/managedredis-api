package com.example.managedredis.service;

import com.example.managedredis.model.ManagedRedis;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

public interface ManagedRedisService {
    ManagedRedis createManagedRedis(ManagedRedis managedRedis);
    ManagedRedis getManagedRedis(String name);
    void deleteManagedRedis(String name);
}

@Service
class ManagedRedisServiceImpl implements ManagedRedisService {

    private final KubernetesClient kubernetesClient;
    private static final String DEFAULT_NAMESPACE = "default";

    public ManagedRedisServiceImpl(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public ManagedRedis createManagedRedis(ManagedRedis managedRedis) {
        return kubernetesClient.resources(ManagedRedis.class)
                .inNamespace(DEFAULT_NAMESPACE)
                .create(managedRedis);
    }

    @Override
    public ManagedRedis getManagedRedis(String name) {
        return kubernetesClient.resources(ManagedRedis.class)
                .inNamespace(DEFAULT_NAMESPACE)
                .withName(name)
                .get();
    }

    @Override
    public void deleteManagedRedis(String name) {
        kubernetesClient.resources(ManagedRedis.class)
                .inNamespace(DEFAULT_NAMESPACE)
                .withName(name)
                .delete();
    }

    public List<ManagedRedis> list(String namespace) {
        String ns = Optional.ofNullable(namespace).orElse(DEFAULT_NAMESPACE);
        return kubernetesClient.resources(ManagedRedis.class)
                .inNamespace(ns)
                .list()
                .getItems();
    }
} 