package com.example.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시성 PoC 테스트
 * - 사전 조건: 애플리케이션이 localhost:8080 에서 실행 중이어야 합니다.
 * - 쿠폰 ID 1번 (재고 300개, 가격 10000)에 500명이 동시에 발급 요청
 * - 회원 500명 중 25명(member10, 30, 50 ... 490)은 포인트 9000 → 포인트 부족으로 실패
 * - 기대 결과: 정확히 300건만 성공 (재고 초과 방지, 포인트 부족 실패 포함)
 */
class CouponConcurrencyTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final int TOTAL_MEMBERS = 500;
    private static final int COUPON_STOCK = 300;
    private static final int INSUFFICIENT_POINT_MEMBERS = 25; // 포인트 부족 회원 수
    private static final long COUPON_ID = 1L;

    @Test
    @DisplayName("500명 동시 쿠폰 발급 요청 시 재고(300개)만큼만 성공해야 한다 (포인트 부족 25명 포함)")
    void concurrentCouponIssue() throws InterruptedException {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        CountDownLatch readyLatch = new CountDownLatch(TOTAL_MEMBERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(TOTAL_MEMBERS);

        ExecutorService executor = Executors.newFixedThreadPool(TOTAL_MEMBERS);

        for (int i = 1; i <= TOTAL_MEMBERS; i++) {
            final long memberId = i;
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(BASE_URL + "/api/coupon?couponId=" + COUPON_ID + "&memberId=" + memberId))
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        long startTime = System.currentTimeMillis();
        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startTime;
        executor.shutdown();
        if (!completed) {
            throw new IllegalStateException("성능 기준 초과 (10초) — 완료되지 않은 요청이 있습니다.");
        }

        int expectedFailCount = TOTAL_MEMBERS - COUPON_STOCK;

        System.out.println("=== 동시성 테스트 결과 ===");
        System.out.println("총 요청 수        : " + TOTAL_MEMBERS);
        System.out.println("성공 건수         : " + successCount.get() + " (기대: " + COUPON_STOCK + ")");
        System.out.println("실패 건수         : " + failCount.get() + " (기대: " + expectedFailCount + ")");
        System.out.println("  - 재고 초과 실패 : " + (expectedFailCount - INSUFFICIENT_POINT_MEMBERS) + "명 이상");
        System.out.println("  - 포인트 부족 실패: " + INSUFFICIENT_POINT_MEMBERS + "명 이상");
        System.out.printf("소요 시간         : %d ms (%d.%03d초)%n", elapsed, elapsed / 1000, elapsed % 1000);
        System.out.println("========================");

        assertThat(successCount.get())
                .as("성공 건수가 재고(100개)와 일치해야 합니다")
                .isEqualTo(COUPON_STOCK);

        assertThat(failCount.get())
                .as("실패 건수가 %d건이어야 합니다 (재고 초과 + 포인트 부족)", expectedFailCount)
                .isEqualTo(expectedFailCount);
    }
}