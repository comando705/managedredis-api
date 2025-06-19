package com.example.managedredis.model;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1alpha1")
@Group("redis.example.com")
public class ManagedRedis extends CustomResource<ManagedRedisSpec, ManagedRedisStatus> implements Namespaced {
    @Override
    protected ManagedRedisSpec initSpec() {
        return new ManagedRedisSpec();
    }

    @Override
    protected ManagedRedisStatus initStatus() {
        return new ManagedRedisStatus();
    }
} 