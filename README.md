# Managed Redis Operator

Redis 클러스터를 쉽게 관리할 수 있는 Kubernetes Operator와 REST API를 제공

## 기능

- Redis 클러스터 생성/조회/삭제
- Primary-Replica 구조 자동 구성
- 상태 모니터링 및 자동 복구
- REST API를 통한 관리
- 네임스페이스 기반 멀티 테넌시 지원

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

## 문제 해결

### 1. 컨테이너 관련 문제
```bash
# 컨테이너 상태 확인
docker ps -a | grep managedredis-api

# 상세 로그 확인
docker logs -f managedredis-api

# 컨테이너 내부 접속
docker exec -it managedredis-api /bin/bash
```

### 2. Kubernetes 연결 문제
```bash
# Kubernetes 연결 상태 확인
kubectl cluster-info
kubectl get nodes

# CRD 상태 확인
kubectl get crd | grep managedredis
```

### 3. 권한 문제
```bash
# RBAC 권한 확인
kubectl auth can-i get managedredis.redis.managed.com
kubectl auth can-i create managedredis.redis.managed.com
```

## 테스트 방법

### 사전 준비 사항

1. Kubernetes CRD 및 권한 설정
```bash
# CRD 설치
kubectl apply -f k8s/managedredis-crd.yaml

# RBAC 권한 설정
kubectl create clusterrolebinding managedredis-admin \
  --clusterrole=cluster-admin \
  --serviceaccount=default:default

# ServiceAccount 토큰 시크릿 생성
kubectl create token default
```

2. Kubernetes 설정 확인
```bash
# CRD 설치 확인
kubectl get crd managedredis.redis.managed.com

# 권한 설정 확인
kubectl auth can-i get managedredis.redis.managed.com
```

### Docker 환경에서 테스트 실행

1. 테스트용 Docker 이미지 빌드
```bash
# 테스트용 이미지 빌드
docker build -t managedredis-api-test:latest -f Dockerfile.test .

# 이미지가 제대로 생성되었는지 확인
docker images | grep managedredis-api-test
```

2. 테스트용 Docker 컨테이너 실행
```bash
# 기존 컨테이너가 있다면 제거
docker rm -f managedredis-api-test

# Kubernetes 설정을 마운트하여 컨테이너 실행
docker run -d --name managedredis-api-test \
  -v ~/.kube/config:/root/.kube/config \
  -v $(pwd):/app \
  -e KUBERNETES_MASTER=https://kubernetes.docker.internal:6443 \
  managedredis-api-test:latest

# 컨테이너 상태 확인
docker ps -a | grep managedredis-api-test
```

3. 컨테이너 내부에서 테스트 실행
```bash
# 컨테이너 접속
docker exec -it managedredis-api-test /bin/bash

# 컨테이너 내부에서 테스트 실행
cd /app
mvn test

# 특정 테스트 클래스 실행
mvn test -Dtest=RedisOperatorTest

# 특정 패키지의 테스트만 실행
mvn test -Dtest="com.example.managedredis.service.*Test"

# 테스트 커버리지 리포트 생성 (JaCoCo)
mvn verify
```

4. 테스트 결과 확인
```bash
# 테스트 결과 및 로그 확인
cat target/surefire-reports/*.txt

# 커버리지 리포트 확인 (호스트에서 자동으로 확인 가능)
# target/site/jacoco/index.html 파일을 브라우저에서 열어 확인
```

5. 테스트 완료 후 정리
```bash
# 컨테이너 종료 및 삭제
docker stop managedredis-api-test
docker rm managedredis-api-test

# 이미지 삭제 (선택사항)
docker rmi managedredis-api-test:latest
```

### 수동 테스트

1. Redis 클러스터 생성 테스트
```bash
# 단일 노드 Redis 생성
curl -X POST http://localhost:8080/api/v1/managedredis \
  -H "Content-Type: application/json" \
  -d @test-redis.json

# 상태 확인
curl http://localhost:8080/api/v1/managedredis/test-redis
```

2. Primary-Replica 구성 테스트
```bash
# Primary-Replica Redis 생성
curl -X POST http://localhost:8080/api/v1/managedredis \
  -H "Content-Type: application/json" \
  -d @k8s/cluster-redis.yaml

# 복제 상태 확인
curl http://localhost:8080/api/v1/managedredis/cluster-redis
```

### 테스트 환경 설정

1. 테스트용 application-test.yml
```yaml
spring:
  profiles: test
kubernetes:
  master: http://localhost:8001
  namespace: test
redis:
  health-check:
    initial-delay: 5
    period: 5
```

## API 문서

Swagger UI를 통해 API 문서를 확인할 수 있습니다:
- http://localhost:8080/swagger-ui.html

### 주요 엔드포인트

- `POST /api/v1/managedredis`: Redis 클러스터 생성
- `GET /api/v1/managedredis`: Redis 클러스터 목록 조회
- `GET /api/v1/managedredis/{name}`: 특정 Redis 클러스터 조회
- `DELETE /api/v1/managedredis/{name}`: Redis 클러스터 삭제