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
```bash
# 소스 코드 클론
git clone git@github.com:comando705/managedredis-api.git
cd managedredis-api

### Docker 환경

1. Docker 이미지 준비
```bash
# 이미지 직접 빌드
docker build -t managedredis-api:latest .

```

2. 실행 방법
A. Docker 단독 컨테이너로 실행
```bash
# 기존 컨테이너 정리 (필요한 경우)
docker rm -f managedredis-api

# Docker Desktop Kubernetes 환경에서 실행
docker run -d \
  --name managedredis-api \
  -p 8080:8080 \
  -v ~/.kube/config:/root/.kube/config \
  -e KUBERNETES_MASTER=https://kubernetes.docker.internal:6443 \
  -e KUBERNETES_NAMESPACE=default \
  managedredis-api:latest

# 컨테이너 로그 확인
docker logs -f managedredis-api

# 컨테이너 중지 및 제거
docker stop managedredis-api
docker rm managedredis-api
```

주의사항:
- Docker Compose와 단독 컨테이너 실행 중 한 가지 방법만 선택하여 사용하세요.
- 동시에 두 방법을 사용하면 포트(8080)와 컨테이너 이름이 충돌합니다.
- 실행 전 항상 이전 컨테이너를 정리하는 것이 좋습니다.

3. 상태 확인
```bash
# 컨테이너 상태 확인
docker ps -a | grep managedredis-api

# API 엔드포인트 테스트
curl http://localhost:8080/actuator/health

# Swagger UI 접속
open http://localhost:8080/swagger-ui.html
```

5. 문제 해결
```bash
# 컨테이너 상세 정보 확인
docker inspect managedredis-api

# 컨테이너 로그 확인
docker logs managedredis-api

# 컨테이너 내부 접속
docker exec -it managedredis-api /bin/bash

# 네트워크 연결 확인
docker exec managedredis-api curl -v http://kubernetes.docker.internal:6443
```

주의사항:
- Docker Desktop의 Kubernetes를 사용하는 경우 `kubernetes.docker.internal`을 사용하세요.
- 외부 Kubernetes 클러스터를 사용하는 경우 적절한 마스터 URL을 설정하세요.
- 보안을 위해 프로덕션 환경에서는 적절한 RBAC 설정과 시크릿 관리가 필요합니다.

### Kubernetes 환경

1. Kubernetes 설정
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

2. 애플리케이션 배포
```bash
# CRD 설치
kubectl apply -f k8s/managedredis-crd.yaml

# 애플리케이션 배포
kubectl apply -f k8s/deployment.yaml
```

### Docker Desktop Kubernetes를 선택한 이유

1. **간편한 설정**
   - 별도의 도구 설치가 필요 없음 (minikube나 kind 불필요)
   - Docker Desktop UI를 통한 쉬운 활성화/비활성화
   - 자동 업데이트 지원

2. **리소스 효율성**
   - Docker와 Kubernetes가 동일한 가상화 환경 공유
   - 시스템 리소스 사용량 최적화
   - 추가적인 VM 오버헤드 없음

3. **개발 환경 일관성**
   - Docker와 Kubernetes 버전 호환성 보장
   - 동일한 컨텍스트에서 Docker와 Kubernetes 명령어 사용
   - 로컬/프로덕션 환경 간 일관성 유지

4. **통합 도구 지원**
   - Docker Desktop Dashboard를 통한 컨테이너/파드 모니터링
   - 내장된 로그 뷰어 및 쉘 액세스
   - 리소스 사용량 모니터링

5. **네트워크 접근성**
   - localhost를 통한 직접 서비스 접근
   - 복잡한 포트 포워딩 설정 불필요
   - 호스트 시스템과의 원활한 네트워크 통합

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

### 로컬 개발 환경에서 테스트 실행 (선택사항)

Maven이 로컬에 설치된 경우에만 사용:

```bash
# Maven 설치 (필요한 경우)
# macOS
brew install maven

# Ubuntu
sudo apt-get install maven

# 테스트 실행
mvn test
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

### 테스트 모범 사례

1. **단위 테스트**
   - 각 컴포넌트의 독립적인 기능 테스트
   - Mockito를 사용한 의존성 모킹
   - JUnit 5 기능 활용

2. **통합 테스트**
   - 실제 환경과 유사한 설정으로 테스트
   - TestContainers를 사용한 통합 테스트
   - 실제 Redis 인스턴스와의 연동 테스트

3. **성능 테스트**
   - JMeter 또는 k6를 사용한 부하 테스트
   - Redis 클러스터의 확장성 테스트
   - 장애 복구 시나리오 테스트

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