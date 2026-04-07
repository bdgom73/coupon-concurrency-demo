# 쿠폰 동시성 제어 데모 프로젝트

> **200명이 동시에 100개 한정 쿠폰에 몰릴 때, 정확히 100건만 발급되는가?**
> 낙관적 락(Optimistic Lock) + 재시도 전략으로 고동시성 쿠폰 발급 시스템을 검증하는 PoC 프로젝트입니다.

---

## 목차

- [배경 및 문제 정의](#배경-및-문제-정의)
- [동시성 제어 전략](#동시성-제어-전략)
- [기술 스택](#기술-스택)
- [프로젝트 구조](#프로젝트-구조)
- [핵심 설계](#핵심-설계)
- [동시성 테스트](#동시성-테스트)
- [실행 방법](#실행-방법)
- [설정 상세](#설정-상세)

---

## 배경 및 문제 정의

### 문제 상황

```
재고: 100개 한정 쿠폰
요청: 200명이 동시에 발급 요청
기대: 정확히 100명 성공, 100명 실패
```

고동시성 환경에서는 아래와 같은 **Race Condition**이 발생합니다.

```
스레드 A: stock = 1 조회
스레드 B: stock = 1 조회
스레드 A: stock > 0 → 발급 성공, stock = 0 저장
스레드 B: stock > 0 → 발급 성공, stock = -1 저장  ← 재고 초과 발급!
```

### 왜 비관적 락(Pessimistic Lock)은 안 됐나

```
시도: SELECT ... FOR UPDATE (비관적 락)
결과: 200개 요청 중 22건만 성공

원인: H2 데이터베이스는 행(row) 수준 락이 아닌 테이블(table) 수준 락을 사용
     → 200개 트랜잭션이 순차 직렬화되어 Connection Timeout 속출
```

---

## 동시성 제어 전략

### 낙관적 락(Optimistic Lock) + 지수 백오프 재시도

```
읽기: 병렬 처리 (락 없음)
     ↓
커밋: @Version 체크 (매우 짧은 락 구간)
     ↓
충돌: ObjectOptimisticLockingFailureException 발생
     ↓
재시도: 0~30ms 랜덤 백오프 후 재시도 (25초 타임아웃)
```

```
비교                   비관적 락          낙관적 락 + 재시도
────────────────────────────────────────────────────────
락 구간               트랜잭션 전체        커밋 순간만
충돌 처리             대기(Block)          예외 → 재시도
H2 환경               테이블 락 → 직렬화   영향 없음
200명 결과            22건 성공           100건 성공 (정확)
```

### 재시도 전략 상세

```java
// 25초 타임아웃 내에서 재시도 반복
long deadline = System.currentTimeMillis() + 25_000;

while (System.currentTimeMillis() < deadline) {
    try {
        return transactionTemplate.execute(status -> {
            // 1. 중복 발급 확인
            // 2. 쿠폰 조회 (재고 확인)
            // 3. 회원 조회 (포인트 확인)
            // 4. 재고 감소 & 포인트 차감
            // 5. 발급 기록 저장
        });
    } catch (ObjectOptimisticLockingFailureException e) {
        // 낙관적 락 충돌 → 랜덤 백오프 후 재시도
        long remaining = deadline - System.currentTimeMillis();
        Thread.sleep((long)(Math.random() * Math.min(30, remaining)));
    } catch (비즈니스예외) {
        throw e; // 재시도 없이 즉시 전파
    }
}

throw new CouponIssueTimeoutException(); // 25초 초과
```

---

## 기술 스택

| 항목 | 내용 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 4.0.5 |
| ORM | Spring Data JPA / Hibernate |
| Database | H2 (In-Memory) |
| Connection Pool | HikariCP |
| Build | Gradle |
| Test | JUnit 5 |

---

## 프로젝트 구조

```
src/
├── main/java/com/example/demo/
│   ├── domain/
│   │   ├── coupon/
│   │   │   ├── controller/   CouponController.java
│   │   │   ├── service/      CouponService.java          ← 핵심 동시성 로직
│   │   │   ├── entity/       Coupon.java, CouponIssue.java
│   │   │   ├── repository/   CouponRepository.java, CouponIssueRepository.java
│   │   │   ├── dto/          CouponIssueRequest.java, CouponIssueResponse.java
│   │   │   └── exception/    CouponNotFoundException, OutOfStock, AlreadyIssued
│   │   └── member/
│   │       ├── entity/       Member.java
│   │       ├── repository/   MemberRepository.java
│   │       └── exception/    MemberNotFoundException, InsufficientPoint
│   └── global/
│       ├── config/           AsyncConfig.java, H2ServerConfig.java
│       ├── exception/        GlobalExceptionHandler.java, ErrorCode.java
│       └── infra/fcm/        FcmNotificationService.java
└── test/java/com/example/demo/
    └── CouponConcurrencyTest.java                         ← 동시성 테스트
```

---

## 핵심 설계

### 1. Coupon 엔티티 — 낙관적 락

```java
@Entity
public class Coupon {

    @Version
    private Long version;   // 낙관적 락: 커밋 시 version 불일치 → 예외 발생

    private Integer stock;

    public void decreaseStock() {
        if (this.stock <= 0) throw new CouponOutOfStockException();
        this.stock--;
    }
}
```

### 2. CouponIssue 엔티티 — 중복 방지

```java
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"coupon_id", "member_id"}))
public class CouponIssue {
    // DB 유니크 제약으로 동일 회원의 중복 발급을 원천 차단
}
```

### 3. 발급 흐름

```
POST /api/coupon?couponId=1&memberId=100
        │
        ▼
[중복 확인] → 이미 발급됨? → 409 COUPON_003
        │
        ▼
[쿠폰 조회] → 없음? → 404 COUPON_001
        │
        ▼
[재고 확인] → stock <= 0? → 409 COUPON_002
        │
        ▼
[회원 조회] → 없음? → 404 MEMBER_001
        │
        ▼
[포인트 확인] → 부족? → 400 MEMBER_002
        │
        ▼
[발급 기록 저장]
        │
        ▼
[FCM 알림 전송] ← @Async (비동기, 응답 블로킹 없음)
        │
        ▼
200 { "couponIssueId": 1 }
```

### 4. 비동기 알림 — 응답 지연 방지

```java
@Service
public class FcmNotificationService {

    @Async
    public void send(Member member) {
        // 실제 FCM API 호출 (시뮬레이션: 500ms)
        // 트랜잭션 커밋을 블로킹하지 않음
    }
}
```

---

## 동시성 테스트

### 테스트 시나리오

```
쿠폰 재고: 100개
요청 인원: 200명 (스레드 200개 동시 실행)

회원 구성:
  - 190명: 포인트 100,000 (충분)
  -  10명: 포인트   9,000 (쿠폰 가격 10,000 → 부족)
           member 10, 30, 50, 70, 90, 110, 130, 150, 170, 190

예상 결과:
  성공: 100건 (재고 한도)
  실패: 100건 (재고 소진 + 포인트 부족)
```

### 테스트 구현

```java
@Test
void 쿠폰_동시성_테스트_200명_요청_100명_성공() throws InterruptedException {
    int totalUsers = 200;
    ExecutorService executor = Executors.newFixedThreadPool(totalUsers);

    CountDownLatch readyLatch = new CountDownLatch(totalUsers); // 준비 대기
    CountDownLatch startLatch = new CountDownLatch(1);          // 일제 시작
    CountDownLatch doneLatch  = new CountDownLatch(totalUsers); // 완료 대기

    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger failCount    = new AtomicInteger();

    for (int i = 1; i <= totalUsers; i++) {
        final int memberId = i;
        executor.submit(() -> {
            readyLatch.countDown();
            startLatch.await();  // 모든 스레드가 준비될 때까지 대기 → 동시성 극대화

            // HTTP 요청: POST /api/coupon?couponId=1&memberId={memberId}
            int status = sendRequest(memberId);
            if (status == 200) successCount.incrementAndGet();
            else               failCount.incrementAndGet();

            doneLatch.countDown();
        });
    }

    readyLatch.await();  // 전원 준비
    startLatch.countDown();  // 동시 출발!
    doneLatch.await(10, TimeUnit.SECONDS);  // 10초 내 완료

    assertThat(successCount.get()).isEqualTo(100);  // 정확히 100건
    assertThat(failCount.get()).isEqualTo(100);
}
```

### 테스트 결과

| 지표 | 값 |
|------|----|
| 전체 요청 | 200건 |
| 성공 | 100건 |
| 실패 (재고 소진) | ~90건 |
| 실패 (포인트 부족) | 10건 |
| 재고 초과 발급 | 0건 |
| 중복 발급 | 0건 |

---

## 실행 방법

### 1. 애플리케이션 실행

```bash
./gradlew bootRun
```

서버 기동 시 `import.sql`이 자동 실행되어 테스트 데이터가 삽입됩니다.

- 쿠폰 1개 (재고 100개, 가격 10,000포인트)
- 회원 200명

### 2. 동시성 테스트 실행

> 반드시 서버를 먼저 실행한 후 테스트를 실행하세요. (HTTP 기반 통합 테스트)

```bash
./gradlew test --tests CouponConcurrencyTest
```

### 3. H2 웹 콘솔

```
URL:      http://localhost:9000
JDBC URL: jdbc:h2:mem:test
Username: sa
Password: (없음)
```

### 4. API 직접 호출

```bash
curl -X POST "http://localhost:8080/api/coupon?couponId=1&memberId=1"
```

**응답 예시**

```json
// 성공
{ "couponIssueId": 1 }

// 재고 소진
{ "code": "COUPON_002", "message": "쿠폰 재고가 소진되었습니다." }

// 중복 발급
{ "code": "COUPON_003", "message": "이미 발급된 쿠폰입니다." }

// 포인트 부족
{ "code": "MEMBER_002", "message": "포인트가 부족합니다." }

// 동시성 혼잡 타임아웃
{ "code": "COUPON_004", "message": "잠시 후 다시 시도해주세요." }
```

---

## 설정 상세

### HikariCP 커넥션 풀

```yaml
spring.datasource.hikari:
  maximum-pool-size: 50   # 고정 크기: 예측 가능한 리소스 관리
  minimum-idle: 50        # 유휴 커넥션 유지 → 획득 지연 없음
  connection-timeout: 28000  # 28초: 재시도 윈도우(25초) + 여유
```

### H2 락 타임아웃

```yaml
# 낙관적 락 충돌 시 DB 내부 대기 최대 시간
spring.datasource.url: jdbc:h2:mem:test;LOCK_TIMEOUT=10000
```

### HTTP 타임아웃

```yaml
server.tomcat.connection-timeout: 3000  # 3초: 클라이언트 요청 처리 한도
```

### 에러 코드 정의

| 코드 | HTTP | 설명 |
|------|------|------|
| COUPON_001 | 404 | 쿠폰 없음 |
| COUPON_002 | 409 | 재고 소진 |
| COUPON_003 | 409 | 중복 발급 |
| COUPON_004 | 503 | 동시성 타임아웃 |
| MEMBER_001 | 404 | 회원 없음 |
| MEMBER_002 | 400 | 포인트 부족 |
