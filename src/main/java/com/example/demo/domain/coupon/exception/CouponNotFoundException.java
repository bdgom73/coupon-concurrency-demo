package com.example.demo.domain.coupon.exception;

public class CouponNotFoundException extends RuntimeException {
    public CouponNotFoundException(Long couponId) {
        super("존재하지 않는 쿠폰입니다. id=" + couponId);
    }
}
