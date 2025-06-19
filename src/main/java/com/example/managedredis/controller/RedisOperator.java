package com.example.managedredis.controller;

import com.example.managedredis.config.RedisConfig;
import com.example.managedredis.model.ManagedRedis;
import com.example.managedredis.model.ManagedRedisStatus;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class RedisOperator {
    private static final Logger log = LoggerFactory.getLogger(RedisOperator.class);

    private final KubernetesClient kubernetesClient;
    private final SharedIndexInformer<ManagedRedis> informer;
    private final RedisConfig redisConfig;
    private ScheduledExecutorService healthCheckExecutor;

    public RedisOperator(KubernetesClient kubernetesClient, RedisConfig redisConfig) {
        this.kubernetesClient = kubernetesClient;
        this.redisConfig = redisConfig;
        this.informer = kubernetesClient.resources(ManagedRedis.class).inAnyNamespace().inform();
    }

    @PostConstruct
    public void initialize() {
        healthCheckExecutor = Executors.newScheduledThreadPool(redisConfig.getHealthCheck().getThreadPoolSize());
        
        informer.addEventHandler(new ResourceEventHandler<ManagedRedis>() {
            @Override
            public void onAdd(ManagedRedis redis) {
                try {
                    redis.getStatus().setPhase("Creating");
                    createRedisCluster(redis);
                    monitorRedisHealth(redis);
                } catch (Exception e) {
                    log.error("Failed to handle Redis cluster creation", e);
                    updateStatus(redis, "Failed");
                }
            }

            @Override
            public void onUpdate(ManagedRedis oldRedis, ManagedRedis newRedis) {
                try {
                    updateRedisCluster(oldRedis, newRedis);
                } catch (Exception e) {
                    log.error("Failed to handle Redis cluster update", e);
                    updateStatus(newRedis, "Failed");
                }
            }

            @Override
            public void onDelete(ManagedRedis redis, boolean deletedFinalStateUnknown) {
                try {
                    deleteRedisCluster(redis);
                } catch (Exception e) {
                    log.error("Failed to handle Redis cluster deletion", e);
                }
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
            try {
                updateStatus(redis, "Failed");
            } catch (Exception statusError) {
                log.error("Failed to update status", statusError);
            }
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
                        String.format("redis-server --port %s --slaveof %s-primary %s", 
                                RedisConfig.REDIS_PORT, name, RedisConfig.REDIS_PORT))
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

        // Update phase
        redis.getStatus().setPhase(phase);

        // Update endpoints
        redis.getStatus().setPrimaryEndpoint(
                String.format("%s-primary.%s.svc:%s", name, namespace, RedisConfig.REDIS_PORT));
        
        if (redis.getSpec().getReplicas() > 1) {
            redis.getStatus().setReaderEndpoint(
                    String.format("%s-reader.%s.svc:%s", name, namespace, RedisConfig.REDIS_PORT));
        }

        // Update node status
        List<ManagedRedisStatus.Node> nodes = new ArrayList<>();
        
        // Check primary node
        String primaryPodName = name + "-0";
        Pod primaryPod = kubernetesClient.pods()
                .inNamespace(namespace)
                .withName(primaryPodName)
                .get();
        
        if (primaryPod != null) {
            ManagedRedisStatus.Node primaryNode = new ManagedRedisStatus.Node();
            primaryNode.setName(primaryPodName);
            primaryNode.setRole(RedisConfig.PRIMARY_ROLE);
            primaryNode.setStatus(isPodReady(primaryPod) ? "Ready" : "NotReady");
            primaryNode.setEndpoint(String.format("%s.%s.pod:%s", 
                    primaryPodName, namespace, RedisConfig.REDIS_PORT));
            nodes.add(primaryNode);
        }

        // Check replica nodes
        for (int i = 1; i < redis.getSpec().getReplicas(); i++) {
            String replicaPodName = name + "-" + i;
            Pod replicaPod = kubernetesClient.pods()
                    .inNamespace(namespace)
                    .withName(replicaPodName)
                    .get();
            
            if (replicaPod != null) {
                ManagedRedisStatus.Node replicaNode = new ManagedRedisStatus.Node();
                replicaNode.setName(replicaPodName);
                replicaNode.setRole(RedisConfig.REPLICA_ROLE);
                replicaNode.setStatus(isPodReady(replicaPod) ? "Ready" : "NotReady");
                replicaNode.setEndpoint(String.format("%s.%s.pod:%s", 
                        replicaPodName, namespace, RedisConfig.REDIS_PORT));
                nodes.add(replicaNode);
            }
        }

        redis.getStatus().setNodes(nodes);

        // Update overall phase based on node status
        if ("Creating".equals(phase)) {
            if (nodes.size() == redis.getSpec().getReplicas() && 
                nodes.stream().allMatch(node -> "Ready".equals(node.getStatus()))) {
                redis.getStatus().setPhase("Running");
            }
        }

        try {
            kubernetesClient.resources(ManagedRedis.class)
                    .inNamespace(namespace)
                    .withName(name)
                    .updateStatus(redis);
        } catch (Exception e) {
            log.error("Failed to update status", e);
        }
    }

    private boolean isPodReady(Pod pod) {
        if (pod.getStatus() == null || pod.getStatus().getConditions() == null) {
            return false;
        }

        return pod.getStatus().getConditions().stream()
                .filter(condition -> "Ready".equals(condition.getType()))
                .findFirst()
                .map(condition -> "True".equals(condition.getStatus()))
                .orElse(false);
    }

    private void monitorRedisHealth(ManagedRedis redis) {
        String namespace = redis.getMetadata().getNamespace();
        String name = redis.getMetadata().getName();

        try {
            // Schedule periodic health checks
            healthCheckExecutor.scheduleAtFixedRate(() -> {
                try {
                    List<Pod> redisPods = kubernetesClient.pods()
                            .inNamespace(namespace)
                            .withLabel(RedisConfig.APP_LABEL, name)
                            .list()
                            .getItems();

                    boolean needsStatusUpdate = false;
                    List<ManagedRedisStatus.Node> nodes = redis.getStatus().getNodes();
                    
                    if (nodes != null) {
                        for (ManagedRedisStatus.Node node : nodes) {
                            Pod pod = redisPods.stream()
                                    .filter(p -> p.getMetadata().getName().equals(node.getName()))
                                    .findFirst()
                                    .orElse(null);

                            if (pod != null) {
                                String currentStatus = isPodReady(pod) ? "Ready" : "NotReady";
                                if (!currentStatus.equals(node.getStatus())) {
                                    node.setStatus(currentStatus);
                                    needsStatusUpdate = true;
                                }
                            }
                        }

                        if (needsStatusUpdate) {
                            updateStatus(redis, redis.getStatus().getPhase());
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to check Redis health", e);
                }
            }, redisConfig.getHealthCheck().getInitialDelay(), 
               redisConfig.getHealthCheck().getPeriod(), 
               TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to start Redis health monitoring", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (healthCheckExecutor != null) {
            healthCheckExecutor.shutdown();
            try {
                if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    healthCheckExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                healthCheckExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
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