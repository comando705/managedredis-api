package com.example.managedredis.controller;

import com.example.managedredis.model.ManagedRedis;
import com.example.managedredis.service.ManagedRedisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/managedredis")
public class ManagedRedisController {

    private final ManagedRedisService managedRedisService;

    public ManagedRedisController(ManagedRedisService managedRedisService) {
        this.managedRedisService = managedRedisService;
    }

    @PostMapping
    public ResponseEntity<ManagedRedis> createManagedRedis(
            @RequestParam(required = false) String namespace,
            @RequestBody ManagedRedis managedRedis) {
        return ResponseEntity.ok(managedRedisService.create(namespace, managedRedis));
    }

    @GetMapping
    public ResponseEntity<List<ManagedRedis>> listManagedRedis(
            @RequestParam(required = false) String namespace) {
        return ResponseEntity.ok(managedRedisService.list(namespace));
    }

    @GetMapping("/{name}")
    public ResponseEntity<ManagedRedis> getManagedRedis(
            @RequestParam(required = false) String namespace,
            @PathVariable String name) {
        return ResponseEntity.ok(managedRedisService.get(namespace, name));
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteManagedRedis(
            @RequestParam(required = false) String namespace,
            @PathVariable String name) {
        managedRedisService.delete(namespace, name);
        return ResponseEntity.noContent().build();
    }
} 