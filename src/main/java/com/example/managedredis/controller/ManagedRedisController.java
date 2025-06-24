package com.example.managedredis.controller;

import com.example.managedredis.model.ManagedRedis;
import com.example.managedredis.service.ManagedRedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/managedredis")
@Tag(name = "ManagedRedis", description = "Redis 클러스터 관리 API")
public class ManagedRedisController {

    private final ManagedRedisService managedRedisService;

    public ManagedRedisController(ManagedRedisService managedRedisService) {
        this.managedRedisService = managedRedisService;
    }

    @PostMapping
    @Operation(
        summary = "Redis 클러스터 생성",
        description = "새로운 Redis 클러스터를 생성"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Redis 클러스터가 성공적으로 생성됨",
        content = @Content(schema = @Schema(implementation = ManagedRedis.class))
    )
    public ManagedRedis createRedis(
            @Parameter(description = "Redis 클러스터 생성 요청 본문")
            @RequestBody ManagedRedis redis) {
        return managedRedisService.createManagedRedis(redis);
    }

    @GetMapping("/{name}")
    @Operation(
        summary = "특정 Redis 클러스터 조회",
        description = "지정된 이름의 Redis 클러스터를 조회"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Redis 클러스터 조회 성공",
        content = @Content(schema = @Schema(implementation = ManagedRedis.class))
    )
    public ManagedRedis getRedis(
            @Parameter(description = "Redis 클러스터 이름")
            @PathVariable String name) {
        return managedRedisService.getManagedRedis(name);
    }

    @DeleteMapping("/{name}")
    @Operation(
        summary = "Redis 클러스터 삭제",
        description = "지정된 이름의 Redis 클러스터를 삭제"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Redis 클러스터 삭제 성공"
    )
    public void deleteRedis(
            @Parameter(description = "삭제할 Redis 클러스터 이름")
            @PathVariable String name) {
        managedRedisService.deleteManagedRedis(name);
    }
}