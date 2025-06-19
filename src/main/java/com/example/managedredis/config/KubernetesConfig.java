package com.example.managedredis.config;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KubernetesConfig {

    @Bean
    public KubernetesClient kubernetesClient() {
        try {
            // Docker Desktop의 Kubernetes API 서버로 직접 연결
            Config config = new ConfigBuilder()
                    .withMasterUrl("https://kubernetes.docker.internal:6443")
                    .withTrustCerts(true)
                    .build();

            return new KubernetesClientBuilder()
                    .withConfig(config)
                    .build();
        } catch (Exception e) {
            // 로컬 kubeconfig 사용 시도
            return new KubernetesClientBuilder().build();
        }
    }
} 