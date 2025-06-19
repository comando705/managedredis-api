package com.example.managedredis.controller;

import com.example.managedredis.config.RedisConfig;
import com.example.managedredis.model.ManagedRedis;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class RedisOperator {
    private static final Logger log = LoggerFactory.getLogger(RedisOperator.class);
    private final KubernetesClient kubernetesClient;
    private final SharedIndexInformer<ManagedRedis> informer;

    public RedisOperator(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
        this.informer = kubernetesClient.resources(ManagedRedis.class)
                .inAnyNamespace()
                .inform();
    }

    @PostConstruct
    public void initialize() {
        informer.addEventHandler(new ResourceEventHandler<ManagedRedis>() {
            @Override
            public void onAdd(ManagedRedis redis) {
                createRedisCluster(redis);
            }

            @Override
            public void onUpdate(ManagedRedis oldRedis, ManagedRedis newRedis) {
                updateRedisCluster(oldRedis, newRedis);
            }

            @Override
            public void onDelete(ManagedRedis redis, boolean deletedFinalStateUnknown) {
                deleteRedisCluster(redis);
            }
        });
    }

    private void createRedisCluster(ManagedRedis redis) {
        String namespace = redis.getMetadata().getNamespace();
        String name = redis.getMetadata().getName();
        log.info("Creating Redis cluster: {}/{}", namespace, name);

        try {
            // Create Primary Pod
            createRedisPod(redis, 0, true);

            // Create Replica Pods
            for (int i = 1; i < redis.getSpec().getReplicas(); i++) {
                createRedisPod(redis, i, false);
            }

            // Create Services
            createRedisServices(redis);

            // Update status
            updateStatus(redis, "Running");
        } catch (Exception e) {
            log.error("Failed to create Redis cluster", e);
            updateStatus(redis, "Failed");
        }
    }

    private void createRedisPod(ManagedRedis redis, int index, boolean isPrimary) {
        String namespace = redis.getMetadata().getNamespace();
        String name = redis.getMetadata().getName();
        String podName = String.format("%s-%d", name, index);
        String role = isPrimary ? RedisConfig.PRIMARY_ROLE : RedisConfig.REPLICA_ROLE;

        Map<String, String> labels = new HashMap<>();
        labels.put(RedisConfig.APP_LABEL, name);
        labels.put(RedisConfig.ROLE_LABEL, role);
        labels.put(RedisConfig.MANAGED_BY_LABEL, RedisConfig.MANAGED_BY);

        Container container = new ContainerBuilder()
                .withName("redis")
                .withImage(String.format(RedisConfig.REDIS_IMAGE, redis.getSpec().getVersion()))
                .withPorts(new ContainerPortBuilder()
                        .withContainerPort(Integer.parseInt(RedisConfig.REDIS_PORT))
                        .withName("redis")
                        .build())
                .withCommand("/bin/sh", "-c")
                .withArgs(isPrimary ? 
                        "redis-server --port " + RedisConfig.REDIS_PORT :
                        String.format("redis-server --port %s --slaveof %s-%d %s", 
                                RedisConfig.REDIS_PORT, name, 0, RedisConfig.REDIS_PORT))
                .build();

        Pod pod = new PodBuilder()
                .withNewMetadata()
                    .withName(podName)
                    .withNamespace(namespace)
                    .withLabels(labels)
                    .withOwnerReferences(createOwnerReference(redis))
                .endMetadata()
                .withNewSpec()
                    .withContainers(container)
                .endSpec()
                .build();

        kubernetesClient.pods()
                .inNamespace(namespace)
                .resource(pod)
                .create();
    }

    private void createRedisServices(ManagedRedis redis) {
        String namespace = redis.getMetadata().getNamespace();
        String name = redis.getMetadata().getName();

        // Primary Service
        Map<String, String> primarySelector = new HashMap<>();
        primarySelector.put(RedisConfig.APP_LABEL, name);
        primarySelector.put(RedisConfig.ROLE_LABEL, RedisConfig.PRIMARY_ROLE);

        Service primaryService = new ServiceBuilder()
                .withNewMetadata()
                    .withName(name + "-primary")
                    .withNamespace(namespace)
                    .withOwnerReferences(createOwnerReference(redis))
                .endMetadata()
                .withNewSpec()
                    .withSelector(primarySelector)
                    .withPorts(new ServicePortBuilder()
                            .withPort(Integer.parseInt(RedisConfig.REDIS_PORT))
                            .withName("redis")
                            .build())
                .endSpec()
                .build();

        kubernetesClient.services()
                .inNamespace(namespace)
                .resource(primaryService)
                .create();

        // Reader Service (for replicas)
        if (redis.getSpec().getReplicas() > 1) {
            Map<String, String> replicaSelector = new HashMap<>();
            replicaSelector.put(RedisConfig.APP_LABEL, name);
            replicaSelector.put(RedisConfig.ROLE_LABEL, RedisConfig.REPLICA_ROLE);

            Service readerService = new ServiceBuilder()
                    .withNewMetadata()
                        .withName(name + "-reader")
                        .withNamespace(namespace)
                        .withOwnerReferences(createOwnerReference(redis))
                    .endMetadata()
                    .withNewSpec()
                        .withSelector(replicaSelector)
                        .withPorts(new ServicePortBuilder()
                                .withPort(Integer.parseInt(RedisConfig.REDIS_PORT))
                                .withName("redis")
                                .build())
                    .endSpec()
                    .build();

            kubernetesClient.services()
                    .inNamespace(namespace)
                    .resource(readerService)
                    .create();
        }
    }

    private void updateStatus(ManagedRedis redis, String phase) {
        String namespace = redis.getMetadata().getNamespace();
        String name = redis.getMetadata().getName();

        redis.getStatus().setPhase(phase);
        redis.getStatus().setPrimaryEndpoint(
                String.format("%s-primary.%s.svc:%s", name, namespace, RedisConfig.REDIS_PORT));
        
        if (redis.getSpec().getReplicas() > 1) {
            redis.getStatus().setReaderEndpoint(
                    String.format("%s-reader.%s.svc:%s", name, namespace, RedisConfig.REDIS_PORT));
        }

        kubernetesClient.resources(ManagedRedis.class)
                .inNamespace(namespace)
                .withName(name)
                .updateStatus(redis);
    }

    private void updateRedisCluster(ManagedRedis oldRedis, ManagedRedis newRedis) {
        // 구현 예정: 스케일링, 버전 업그레이드 등
        log.info("Update not implemented yet");
    }

    private void deleteRedisCluster(ManagedRedis redis) {
        String namespace = redis.getMetadata().getNamespace();
        String name = redis.getMetadata().getName();
        log.info("Deleting Redis cluster: {}/{}", namespace, name);

        // Kubernetes의 Owner Reference를 통해 자동으로 삭제됨
    }

    private OwnerReference createOwnerReference(ManagedRedis redis) {
        return new OwnerReferenceBuilder()
                .withApiVersion(redis.getApiVersion())
                .withKind(redis.getKind())
                .withName(redis.getMetadata().getName())
                .withUid(redis.getMetadata().getUid())
                .withBlockOwnerDeletion(true)
                .withController(true)
                .build();
    }
}