package com.example.demo.domain.coupon.exception;

public class CouponAlreadyIssuedException extends RuntimeException {
    public CouponAlreadyIssuedException(Long couponId, Long memberId) {
        super("이미 발급된 쿠폰입니다. couponId=" + couponId + ", memberId=" + memberId);
    }
}
