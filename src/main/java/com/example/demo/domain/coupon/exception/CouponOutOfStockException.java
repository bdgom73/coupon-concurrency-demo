package com.example.demo.domain.coupon.exception;

public class CouponOutOfStockException extends RuntimeException {
    public CouponOutOfStockException() {
        super("쿠폰 재고가 소진되었습니다.");
    }
}
