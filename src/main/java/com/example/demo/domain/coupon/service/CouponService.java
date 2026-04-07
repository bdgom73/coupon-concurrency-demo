package com.example.demo.domain.coupon.service;

import com.example.demo.domain.coupon.dto.CouponIssueRequest;
import com.example.demo.domain.coupon.dto.CouponIssueResponse;
import com.example.demo.domain.coupon.entity.Coupon;
import com.example.demo.domain.coupon.entity.CouponIssue;
import com.example.demo.domain.coupon.exception.CouponAlreadyIssuedException;
import com.example.demo.domain.coupon.exception.CouponNotFoundException;
import com.example.demo.domain.coupon.repository.CouponIssueRepository;
import com.example.demo.domain.coupon.repository.CouponRepository;
import com.example.demo.domain.member.entity.Member;
import com.example.demo.domain.member.exception.MemberNotFoundException;
import com.example.demo.domain.member.repository.MemberRepository;
import com.example.demo.global.infra.fcm.FcmNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final MemberRepository memberRepository;
    private final CouponIssueRepository couponIssueRepository;
    private final FcmNotificationService fcmNotificationService;
    private final TransactionTemplate transactionTemplate;

    private static final long RETRY_TIMEOUT_MS = 25_000;
    private static final int  MAX_BACKOFF_MS   = 30;

    public CouponService(CouponRepository couponRepository,
                         MemberRepository memberRepository,
                         CouponIssueRepository couponIssueRepository,
                         FcmNotificationService fcmNotificationService,
                         PlatformTransactionManager transactionManager) {
        this.couponRepository      = couponRepository;
        this.memberRepository      = memberRepository;
        this.couponIssueRepository = couponIssueRepository;
        this.fcmNotificationService = fcmNotificationService;
        this.transactionTemplate   = new TransactionTemplate(transactionManager);
    }

    /**
     * 낙관적 락(@Version) + 재시도 전략으로 동시성 제어
     *
     * 비관적 락 미사용 이유:
     *   H2는 테이블 수준 락을 사용하므로 UPDATE 시 테이블 전체가 잠김
     *   → 200 트랜잭션이 직렬화되어 커넥션 타임아웃 발생 (22건만 성공)
     *
     * 낙관적 락 + 재시도:
     *   읽기(병렬) → 커밋 시 version 체크 → 충돌 시 백오프 후 재시도
     *   커밋 락 구간이 매우 짧아 처리량이 높음
     */
    public CouponIssueResponse issueCoupon(CouponIssueRequest request) {
        long deadline = System.currentTimeMillis() + RETRY_TIMEOUT_MS;

        while (System.currentTimeMillis() < deadline) {
            try {
                Long couponIssueId = transactionTemplate.execute(status -> {
                    Long couponId  = request.couponId();
                    Long memberId  = request.memberId();

                    if (couponIssueRepository.existsByCouponIdAndMemberId(couponId, memberId)) {
                        throw new CouponAlreadyIssuedException(couponId, memberId);
                    }

                    Coupon coupon = couponRepository.findById(couponId)
                            .orElseThrow(() -> new CouponNotFoundException(couponId));

                    Member member = memberRepository.findById(memberId)
                            .orElseThrow(() -> new MemberNotFoundException(memberId));

                    coupon.decreaseStock();
                    member.deductPoint(coupon.getCost());

                    CouponIssue couponIssue = couponIssueRepository.save(new CouponIssue(null, coupon, member));
                    fcmNotificationService.send(member);
                    return couponIssue.getId();
                });

                return CouponIssueResponse.of(couponIssueId);

            } catch (CouponAlreadyIssuedException | CouponNotFoundException | MemberNotFoundException e) {
                throw e; // 재시도 불필요한 비즈니스 예외는 즉시 전파

            } catch (ObjectOptimisticLockingFailureException e) {
                log.debug("낙관적 락 충돌, 재시도 — memberId={}", request.memberId());
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) break;
                try {
                    Thread.sleep((long) (Math.random() * Math.min(MAX_BACKOFF_MS, remaining)));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("재시도 중 인터럽트 발생");
                }
            }
        }

        throw new IllegalStateException("동시 요청 혼잡으로 발급에 실패했습니다. 다시 시도해주세요.");
    }
}
