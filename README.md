# Managed Redis Operator

이 프로젝트는 Kubernetes 환경에서 Redis 인스턴스를 관리하기 위한 커스텀 컨트롤러와 REST API 서버를 제공합니다.

## 아키텍처

이 프로젝트는 다음과 같은 주요 컴포넌트로 구성됩니다:

1. **Custom Resource Definition (CRD)**: Redis 인스턴스의 구성을 정의
2. **Kubernetes Operator**: ManagedRedis 리소스의 라이프사이클 관리
3. **REST API 서버**: 사용자 요청을 처리하고 Kubernetes API와 통신

### CRD 설계

ManagedRedis CRD는 다음과 같은 주요 필드를 포함합니다:

- `spec.version`: Redis 버전
- `spec.replicas`: Redis 인스턴스 수 (1: 단일 노드, >1: Primary-Replica 구성)
- `spec.resources`: 리소스 요구사항 (CPU, 메모리)

상태 정보는 다음 필드에서 확인할 수 있습니다:

- `status.phase`: 현재 클러스터 상태
- `status.primaryEndpoint`: Primary 노드 접속 정보
- `status.readerEndpoint`: Replica 노드 접속 정보
- `status.nodes`: 각 노드의 상세 정보

## 개발 환경 설정

### 필수 요구사항

- JDK 11 이상
- Maven
- Kubernetes 클러스터 (로컬 테스트의 경우 minikube 사용 가능)
- kubectl

### 빌드 및 실행

1. 프로젝트 빌드:
```bash
mvn clean package
```

2. CRD 설치:
```bash
kubectl apply -f k8s/managedredis-crd.yaml
```

3. 애플리케이션 실행:
```bash
java -jar target/managedredis-api-1.0-SNAPSHOT.jar
```

## API 엔드포인트

REST API는 다음과 같은 주요 엔드포인트를 제공합니다:

- `POST /api/v1/managedredis`: 새로운 Redis 인스턴스 생성
- `GET /api/v1/managedredis`: 모든 Redis 인스턴스 조회
- `GET /api/v1/managedredis/{name}`: 특정 Redis 인스턴스 조회
- `DELETE /api/v1/managedredis/{name}`: Redis 인스턴스 삭제

## 구현 세부사항

### 확장성 고려사항

1. API 버전 관리
   - URL 경로에 버전 포함 (예: /api/v1/)
   - API 변경 시 하위 호환성 유지

2. 성능 최적화
   - Redis 인스턴스 상태 조회 시 캐싱 적용
   - Watch API 사용으로 불필요한 폴링 최소화

3. 고가용성
   - Controller Leader Election 구현
   - 상태 업데이트의 멱등성 보장

### 보안 고려사항

1. RBAC
   - 최소 권한 원칙 적용
   - 서비스 어카운트 권한 제한

2. 네트워크 보안
   - Redis 인스턴스간 통신 암호화
   - 접근 제어를 위한 NetworkPolicy 적용 