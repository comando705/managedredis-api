# Managed Redis Operator

Redis 클러스터를 쉽게 관리할 수 있는 Kubernetes Operator와 REST API를 제공합니다.

## 기능

- Redis 클러스터 생성/조회/삭제
- Primary-Replica 구조 자동 구성
- 상태 모니터링 및 자동 복구
- REST API를 통한 관리
- 네임스페이스 기반 멀티 테넌시 지원

## 실행 방법

### 로컬 개발 환경

1. 소스 코드 클론
```bash
git clone [repository-url]
cd managedredis-api
```

2. 로컬 Kubernetes 클러스터 준비
```bash
# minikube 사용 시
minikube start

# kind 사용 시
kind create cluster
```

3. CRD 설치
```bash
kubectl apply -f k8s/managedredis-crd.yaml
```

4. 애플리케이션 실행
```bash
# Maven으로 직접 실행
mvn spring-boot:run

# 또는 JAR 파일로 실행
mvn clean package
java -jar target/managedredis-api.jar
```

### Docker 환경

1. Docker 이미지 빌드
```bash
docker build -t managedredis-api:latest .
```

2. Docker 컨테이너 실행
```bash
docker run -p 8080:8080 \
  -e KUBERNETES_MASTER=https://kubernetes.default.svc \
  -e KUBERNETES_NAMESPACE=default \
  managedredis-api:latest
```

### Kubernetes 환경

1. Kubernetes 클러스터에 배포
```bash
# CRD 설치
kubectl apply -f k8s/managedredis-crd.yaml

# 애플리케이션 배포
kubectl apply -f k8s/deployment.yaml
```

## 테스트 방법

### 단위 테스트 실행

```bash
# 전체 테스트 실행
mvn test

# 특정 테스트 클래스 실행
mvn test -Dtest=RedisOperatorTest

# 테스트 커버리지 리포트 생성
mvn verify
```

### 통합 테스트

1. Redis 클러스터 생성 테스트
```bash
# 단일 노드 Redis 생성
curl -X POST http://localhost:8080/api/v1/managedredis \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-redis",
    "namespace": "default",
    "spec": {
      "version": "6.2.0",
      "replicas": 1
    }
  }'

# 상태 확인
curl http://localhost:8080/api/v1/managedredis/test-redis
```

2. Primary-Replica 구성 테스트
```bash
# Primary-Replica Redis 생성
curl -X POST http://localhost:8080/api/v1/managedredis \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-redis-cluster",
    "namespace": "default",
    "spec": {
      "version": "6.2.0",
      "replicas": 3
    }
  }'

# 복제 상태 확인
curl http://localhost:8080/api/v1/managedredis/test-redis-cluster
```

3. 부하 테스트
```bash
# Redis 클러스터에 부하 생성
redis-benchmark -h <redis-host> -p <redis-port> -n 100000 -c 50
```

### 상태 확인

1. Pod 상태 확인
```bash
kubectl get pods -l app=managedredis
```

2. 로그 확인
```bash
# 애플리케이션 로그
kubectl logs -l app=managedredis-api

# Redis 로그
kubectl logs -l app=managedredis-instance
```

3. Redis 연결 테스트
```bash
# Redis CLI 접속
kubectl exec -it <redis-pod-name> -- redis-cli

# 기본 명령어 테스트
> PING
PONG
> SET key value
OK
> GET key
"value"
```

### 문제 해결 테스트

1. 장애 복구 테스트
```bash
# Redis Pod 강제 종료
kubectl delete pod <redis-pod-name>

# 자동 복구 확인
kubectl get pods -w
```

2. 네트워크 격리 테스트
```bash
# 네트워크 정책 적용
kubectl apply -f k8s/network-policy.yaml

# 연결 테스트
kubectl exec -it <test-pod> -- curl http://managedredis-api:8080
```

## API 문서

Swagger UI를 통해 API 문서를 확인할 수 있습니다:
- http://localhost:8080/swagger-ui.html

### 주요 엔드포인트

- `POST /api/v1/managedredis`: Redis 클러스터 생성
- `GET /api/v1/managedredis`: Redis 클러스터 목록 조회
- `GET /api/v1/managedredis/{name}`: 특정 Redis 클러스터 조회
- `DELETE /api/v1/managedredis/{name}`: Redis 클러스터 삭제

## 설정

### application.yml

```yaml
redis:
  health-check:
    initial-delay: 30  # 초기 지연 시간 (초)
    period: 30         # 체크 주기 (초)
    thread-pool-size: 1  # 상태 체크 스레드 풀 크기
```

### 환경 변수

- `KUBERNETES_MASTER`: Kubernetes API 서버 주소
- `KUBERNETES_NAMESPACE`: 기본 네임스페이스

## 아키텍처

### 컴포넌트

1. REST API (ManagedRedisController)
   - Redis 클러스터 관리를 위한 HTTP 엔드포인트 제공
   - 네임스페이스 기반 멀티 테넌시 지원

2. Kubernetes Operator (RedisOperator)
   - Redis 클러스터 생명주기 관리
   - 상태 모니터링 및 자동 복구
   - 이벤트 기반 동작

3. 상태 관리
   - Pod 상태 모니터링
   - Redis 노드 상태 체크
   - 주기적인 상태 업데이트

### 상태 흐름

1. Creating: 초기 생성 상태
2. Running: 모든 노드가 Ready 상태
3. Failed: 에러 발생 상태

## 모니터링

### 상태 체크

- 30초마다 자동 상태 체크
- Pod Ready 상태 확인
- Redis 노드 역할 및 연결 상태 확인

### 로깅

- 애플리케이션 로그: 표준 Spring Boot 로깅
- 상태 변경 로그: 상태 변경 시마다 기록
- 에러 로그: 문제 발생 시 상세 정보 기록

## 문제 해결

### 일반적인 문제

1. Pod가 생성되지 않는 경우
   - CRD 설치 확인
   - RBAC 권한 확인
   - 네임스페이스 존재 여부 확인

2. 상태 업데이트가 되지 않는 경우
   - 로그 확인
   - Kubernetes API 서버 연결 확인
   - 권한 설정 확인

## 라이선스

Apache License 2.0 