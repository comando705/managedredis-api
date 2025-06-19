package com.example.managedredis.controller;

import com.example.managedredis.model.ManagedRedis;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/managedredis")
@Tag(name = "ManagedRedis", description = "Redis 클러스터 관리 API")
public class ManagedRedisController {

    private final KubernetesClient client;

    public ManagedRedisController(KubernetesClient client) {
        this.client = client;
    }

    @PostMapping
    @Operation(
        summary = "Redis 클러스터 생성",
        description = "새로운 Redis 클러스터를 생성합니다. 기본적으로 1개의 Primary와 지정된 수의 Replica로 구성됩니다."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Redis 클러스터가 성공적으로 생성됨",
        content = @Content(schema = @Schema(implementation = ManagedRedis.class))
    )
    public ManagedRedis createRedis(
            @Parameter(description = "Redis 클러스터 생성 요청 본문")
            @RequestBody ManagedRedis redis,
            @Parameter(description = "생성할 네임스페이스 (선택사항, 기본값: default)")
            @RequestParam(required = false, defaultValue = "default") String namespace) {
        redis.getMetadata().setNamespace(namespace);
        return client.resources(ManagedRedis.class).inNamespace(namespace).create(redis);
    }

    @GetMapping
    @Operation(
        summary = "Redis 클러스터 목록 조회",
        description = "지정된 네임스페이스의 모든 Redis 클러스터 목록을 조회합니다."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Redis 클러스터 목록 조회 성공",
        content = @Content(schema = @Schema(implementation = ManagedRedis.class))
    )
    public List<ManagedRedis> listRedis(
            @Parameter(description = "조회할 네임스페이스 (선택사항, 기본값: default)")
            @RequestParam(required = false, defaultValue = "default") String namespace) {
        return client.resources(ManagedRedis.class).inNamespace(namespace).list().getItems();
    }

    @GetMapping("/{name}")
    @Operation(
        summary = "특정 Redis 클러스터 조회",
        description = "지정된 이름과 네임스페이스의 Redis 클러스터를 조회합니다."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Redis 클러스터 조회 성공",
        content = @Content(schema = @Schema(implementation = ManagedRedis.class))
    )
    public ManagedRedis getRedis(
            @Parameter(description = "Redis 클러스터 이름")
            @PathVariable String name,
            @Parameter(description = "조회할 네임스페이스 (선택사항, 기본값: default)")
            @RequestParam(required = false, defaultValue = "default") String namespace) {
        return client.resources(ManagedRedis.class).inNamespace(namespace).withName(name).get();
    }

    @DeleteMapping("/{name}")
    @Operation(
        summary = "Redis 클러스터 삭제",
        description = "지정된 이름과 네임스페이스의 Redis 클러스터를 삭제합니다."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Redis 클러스터 삭제 성공"
    )
    public void deleteRedis(
            @Parameter(description = "삭제할 Redis 클러스터 이름")
            @PathVariable String name,
            @Parameter(description = "삭제할 네임스페이스 (선택사항, 기본값: default)")
            @RequestParam(required = false, defaultValue = "default") String namespace) {
        client.resources(ManagedRedis.class).inNamespace(namespace).withName(name).delete();
    }
} 