# Managed Redis Operator

Redis 클러스터를 쉽게 관리할 수 있는 Kubernetes Operator와 REST API를 제공합니다.

## 프로젝트 개요

이 프로젝트는 Kubernetes 환경에서 Redis 클러스터를 쉽게 생성, 관리, 모니터링할 수 있는 Operator와 REST API를 제공합니다. Kubernetes Custom Resource Definition(CRD)을 사용하여 Redis 클러스터를 정의하고, Kubernetes API를 통해 관리합니다.

## 주요 기능

- Redis 클러스터 생성/조회/삭제
- Primary-Replica 구조 자동 구성
- 상태 모니터링 및 자동 복구
- REST API를 통한 관리
- 네임스페이스 기반 멀티 테넌시 지원

## 아키텍처

이 프로젝트는 다음과 같은 구성 요소로 이루어져 있습니다:

1. **ManagedRedis CRD**: Redis 클러스터를 정의하는 Kubernetes Custom Resource Definition
2. **REST API**: Redis 클러스터를 관리하기 위한 RESTful API
3. **Redis Operator**: Redis 클러스터의 생성, 관리, 모니터링을 담당하는 컨트롤러

## 기술 스택

- Java 11+
- Spring Boot
- Kubernetes Java Client (fabric8)
- Redis
- Docker
- Kubernetes

## 실행 준비

### 1. Kubernetes 환경 준비
```bash
# Docker Desktop에서 Kubernetes 활성화
1. Docker Desktop 실행
2. Settings 클릭
3. Kubernetes 메뉴 선택
4. "Enable Kubernetes" 체크박스 선택
5. "Apply & Restart" 클릭
6. Kubernetes가 실행될 때까지 대기 (하단 상태바가 녹색으로 변할 때까지)

# Kubernetes 활성화 확인
kubectl cluster-info
kubectl get nodes
```

### 2. CRD 및 권한 설정
```bash
# CRD 설치
kubectl apply -f k8s/managedredis-crd.yaml

# RBAC 권한 설정
kubectl create clusterrolebinding managedredis-admin \
  --clusterrole=cluster-admin \
  --serviceaccount=default:default

# ServiceAccount 토큰 시크릿 생성
kubectl create token default

# 설정 확인
kubectl get crd managedredis.redis.managed.com
kubectl auth can-i get managedredis.redis.managed.com
```

## 실행 방법

### Docker 실행 단계

1. 소스 코드 준비
```bash
git clone git@github.com:comando705/managedredis-api.git
cd managedredis-api
```

2. Docker 이미지 빌드
```bash
docker build -t managedredis-api:latest .
```

3. 기존 컨테이너 정리 (필요한 경우)
```bash
docker rm -f managedredis-api
```

4. 컨테이너 실행
```bash
docker run -d \
  --name managedredis-api \
  -p 8080:8080 \
  -v ~/.kube/config:/root/.kube/config \
  -e KUBERNETES_MASTER=https://kubernetes.docker.internal:6443 \
  -e KUBERNETES_NAMESPACE=default \
  managedredis-api:latest
```

5. 실행 확인
```bash
# 컨테이너 상태 확인
docker ps -a | grep managedredis-api

# 로그 확인
docker logs -f managedredis-api

# API 상태 확인
curl http://localhost:8080/actuator/health

# Swagger UI 접속
open http://localhost:8080/swagger-ui.html
```

## API 사용 방법

### Redis 클러스터 생성

```bash
curl -X POST http://localhost:8080/api/v1/managedredis \
  -H "Content-Type: application/json" \
  -d '{
    "apiVersion": "redis.managed.com/v1",
    "kind": "ManagedRedis",
    "metadata": {
      "name": "my-redis"
    },
    "spec": {
      "version": "6.2",
      "replicas": 3,
      "resources": {
        "requests": {
          "cpu": "100m",
          "memory": "128Mi"
        },
        "limits": {
          "cpu": "200m",
          "memory": "256Mi"
        }
      }
    }
  }'
```

### Redis 클러스터 목록 조회

```bash
curl http://localhost:8080/api/v1/managedredis
```

### 특정 Redis 클러스터 조회

```bash
curl http://localhost:8080/api/v1/managedredis/my-redis
```

### Redis 클러스터 삭제

```bash
curl -X DELETE http://localhost:8080/api/v1/managedredis/my-redis
```

## API 문서

Swagger UI를 통해 API 문서를 확인할 수 있습니다:
- http://localhost:8080/swagger-ui.html

### 주요 엔드포인트

- `POST /api/v1/managedredis`: Redis 클러스터 생성
- `GET /api/v1/managedredis`: Redis 클러스터 목록 조회
- `GET /api/v1/managedredis/{name}`: 특정 Redis 클러스터 조회
- `DELETE /api/v1/managedredis/{name}`: Redis 클러스터 삭제