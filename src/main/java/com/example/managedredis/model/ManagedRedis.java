package com.example.managedredis.model;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import java.util.ArrayList;

@Group("redis.managed.com")
@Version("v1")
public class ManagedRedis extends CustomResource<ManagedRedisSpec, ManagedRedisStatus> implements Namespaced {
    @Override
    protected ManagedRedisSpec initSpec() {
        return new ManagedRedisSpec();
    }

    @Override
    protected ManagedRedisStatus initStatus() {
        ManagedRedisStatus status = new ManagedRedisStatus();
        status.setPhase("Pending");
        status.setNodes(new ArrayList<>());
        return status;
    }
} 